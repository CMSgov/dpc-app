package main

import (
	"bytes"
	"context"
	"fmt"
	"os"
	"time"

	log "github.com/sirupsen/logrus"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials/stscreds"
	"github.com/aws/aws-sdk-go-v2/feature/s3/manager"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/sts"
	"github.com/aws/smithy-go/logging"

	"github.com/ianlopshire/go-fixedwidth"
)

var isTesting = os.Getenv("IS_TESTING") == "true"

var createConnectionVar = createConnection

// Used for dependency injection.  Allows us to easily mock these function in unit tests.
type (
	// s3manager.NewUploader.upload
	S3Uploader func(ctx context.Context, input *s3.PutObjectInput, opts ...func(*manager.Uploader)) (*manager.UploadOutput, error)

	// fixedWidth.Marshal
	FileMarshaler func(v interface{}) ([]byte, error)
	// fixedWidth.Unmarshal
	FileUnmarshaler func(data []byte, v interface{}) error
)

func main() {
	if isTesting {
		filename := "bfdeft01/dpc/in/T.NGD.DPC.RSP.D240123.T1122001.IN"
		createdOptOutCount, createdOptInCount, confirmationFileName, err := importResponseFile("demo-bucket", filename)
		if err != nil {
			log.Error(err)
		} else {
			log.Infof("Created %d opt outs, %d opt ins, and generated confirmation %s", createdOptOutCount, createdOptInCount, confirmationFileName)
		}
	} else {
		lambda.Start(handler)
	}
}

func handler(ctx context.Context, sqsEvent events.SQSEvent) (string, error) {
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})

	s3Event, err := ParseSQSEvent(sqsEvent)

	if err != nil {
		log.Errorf("Failed to parse S3 event: %v", err)
		return "", err
	} else if s3Event == nil {
		log.Info("No S3 event found, skipping safely.")
		return "", nil
	}

	for _, e := range s3Event.Records {
		if e.EventName == "ObjectCreated:Put" {
			createdOptOutCount, createdOptInCount, confirmationFileName, err := importResponseFile(e.S3.Bucket.Name, e.S3.Object.Key)
			logger := log.WithFields(log.Fields{
				"response_filename":      e.S3.Object.Key,
				"created_opt_outs_count": createdOptOutCount,
				"created_opt_ins_count":  createdOptInCount,
				"confirmation_filename":  confirmationFileName,
			})

			if err != nil {
				logger.Errorf("Failed to import response file: %s", err)
				return e.S3.Object.Key, err
			}

			logger.Info("Successfully imported response file and uploaded confirmation file")

			err = deleteS3File(e.S3.Bucket.Name, e.S3.Object.Key)
			if err != nil {
				logger.Errorf("Failed to delete response file after import: %s", err)
			}
			return e.S3.Object.Key, err
		}
	}

	log.Info("No ObjectCreated:Put events found, skipping safely.")
	return "", nil
}

func importResponseFile(bucket string, file string) (int, int, string, error) {
	log.Infof("Importing opt out file: %s (bucket: %s)", file, bucket)
	metadata, err := ParseMetadata(bucket, file)
	if err != nil {
		log.Warningf("Failed to parse opt out file metadata: %s", err)
		return 0, 0, "", err
	}

	dbuser := fmt.Sprintf("/dpc/%s/consent/db_user_dpc_consent", os.Getenv("ENV"))
	dbpassword := fmt.Sprintf("/dpc/%s/consent/db_pass_dpc_consent", os.Getenv("ENV"))
	secrets, err := getConsentDbSecrets(dbuser, dbpassword)
	if err != nil {
		log.Warningf("Failed to get DB secrets: %s", err)
		return 0, 0, "", err
	}

	db, err := createConnectionVar(secrets[dbuser], secrets[dbpassword])
	if err != nil {
		log.Warningf("Failed to create db connection: %s", err)
		return 0, 0, "", err
	}
	if err := db.Ping(); err != nil {
		log.Warningf("Ping error: %s", err)
		return 0, 0, "", err
	}
	defer db.Close()

	optOutFileEntity, err := insertResponseFileMetadata(db, &metadata)
	if err != nil {
		log.Warningf("Failed to insert opt out metadata: %s", err)
		return 0, 0, "", err
	}

	bytes, err := downloadS3File(bucket, file)
	if err != nil {
		log.Warningf("Failed to download opt out file from S3: %s", err)
		if updateStatusErr := updateResponseFileImportStatus(db, optOutFileEntity.id, ImportFail); updateStatusErr != nil {
			return 0, 0, "", updateStatusErr
		}
		return 0, 0, "", err
	}

	records, err := ParseConsentRecords(&metadata, bytes)
	if err != nil {
		log.Warningf("Failed to parse consent records: %s", err)
		if updateStatusErr := updateResponseFileImportStatus(db, optOutFileEntity.id, ImportFail); updateStatusErr != nil {
			return 0, 0, "", updateStatusErr
		}
		return 0, 0, "", err
	}

	createdRecords, err := insertConsentRecords(db, optOutFileEntity.id, records)

	createdOptOutCount := 0
	createdOptInCount := 0

	log.Info("Created consent records with the following ID fields:")
	for _, rec := range createdRecords {
		log.Infof("ID: %s, Opt Out Preference: %s", rec.ID, rec.PolicyCode)
		if rec.PolicyCode == "OPTIN" {
			createdOptInCount++
		} else if rec.PolicyCode == "OPTOUT" {
			createdOptOutCount++
		} else {
			log.Warningf("Unknown policy code saved to database for consent record %s", rec.ID)
		}
	}

	if err != nil {
		log.Warningf("Failed to insert consent records: %s", err)
		return createdOptOutCount, createdOptInCount, "", err
	}

	confirmationFileName := GenerateConfirmationFileName(file, time.Now())
	confirmationFile, err := generateConfirmationFile(true, createdRecords, fixedwidth.Marshal)
	if err != nil {
		log.Warningf("Failed to generate confirmation file: %s", err)
		return createdOptOutCount, createdOptInCount, confirmationFileName, err
	}

	if sess, err := createSession(); err != nil {
		log.Warning("Failed to create session for uploading confirmation file")
		return createdOptOutCount, createdOptInCount, confirmationFileName, err
	} else {
		if err = uploadConfirmationFile(bucket, confirmationFileName, manager.NewUploader(s3.NewFromConfig(sess)).Upload, confirmationFile); err != nil {
			log.Warning("Failed to write upload confirmation file")
			return createdOptOutCount, createdOptInCount, confirmationFileName, err
		}
	}

	return createdOptOutCount, createdOptInCount, confirmationFileName, err
}

var createSession = func() (aws.Config, error) {
	if isTesting {
		return config.LoadDefaultConfig(context.TODO(),
			config.WithSharedConfigProfile("default"),
			config.WithRegion("us-east-1"),
			config.WithEndpointResolver(
				aws.EndpointResolverFunc(func(service, region string) (aws.Endpoint, error) {
					return aws.Endpoint{
						PartitionID:   "aws",
						URL:           "http://localstack:4566",
						SigningRegion: "us-east-1",
					}, nil
				}),
			),
		)
	}
	cfg, err := config.LoadDefaultConfig(context.TODO(), config.WithRegion("us-east-1"), config.WithLogger(logging.Nop{}))
	if err != nil {
		return cfg, err
	}

	assumeRoleArn, err := getAssumeRoleArn()
	if err != nil {
		return cfg, err
	}

	client := sts.NewFromConfig(cfg)
	creds := stscreds.NewAssumeRoleProvider(client, assumeRoleArn)
	cfg.Credentials = aws.NewCredentialsCache(creds)
	return cfg, nil
}

func downloadS3File(bucket string, file string) ([]byte, error) {
	cfg, err := createSession()
	if err != nil {
		return []byte{}, err
	}
	client := s3.NewFromConfig(cfg)
	ctx := context.TODO()
	headObject, headErr := client.HeadObject(ctx, &s3.HeadObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(file),
	})
	if headErr != nil {
		return []byte{}, headErr
	}

	downloader := manager.NewDownloader(client)
	buff := make([]byte, int(headObject.ContentLength))
	w := manager.NewWriteAtBuffer(buff)
	numBytes, err := downloader.Download(context.TODO(), w, &s3.GetObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(file),
	})

	if err == nil {
		log.Printf("file downloaded: size=%d", numBytes)
	}

	return buff.Bytes(), err
}

func generateConfirmationFile(successful bool, records []*OptOutRecord, marshaler FileMarshaler) ([]byte, error) {
	fileCreationDate := time.Now().Format("20060102")
	fileHeader := FileHeader{
		HeaderCode:       "HDR_BENECONFIRM",
		FileCreationDate: fileCreationDate,
	}

	fileTrailer := FileTrailer{
		TrailerCode:       "TRL_BENECONFIRM",
		FileCreationDate:  fileCreationDate,
		DetailRecordCount: fmt.Sprintf("%010d", len(records)),
	}

	var rows []ConfirmationFileRow
	for _, record := range records {
		sharingPreference := "N"
		if record.PolicyCode == "OPTIN" {
			sharingPreference = "Y"
		}
		recordStatus := "Accepted"
		reasonCode := "00"
		if !successful {
			recordStatus = "Rejected"
			reasonCode = "02"
		}

		row := ConfirmationFileRow{
			MBI:               record.MBI,
			EffectiveDate:     record.EffectiveDt.Format("20060102"),
			SharingPreference: sharingPreference,
			RecordStatus:      recordStatus,
			ReasonCode:        reasonCode,
		}

		rows = append(rows, row)
	}

	formattedHeader, err := marshaler(fileHeader)
	if err != nil {
		return []byte{}, err
	}
	formattedHeader = append(formattedHeader, "\n"...)
	formattedRows, err := marshaler(rows)
	if err != nil {
		return []byte{}, err
	}
	formattedRows = append(formattedRows, "\n"...)
	formattedTrailer, err := marshaler(fileTrailer)
	if err != nil {
		return []byte{}, err
	}

	output := append(formattedHeader, formattedRows...)
	return append(output, formattedTrailer...), nil
}

func uploadConfirmationFile(bucket string, file string, uploader S3Uploader, confirmationFile []byte) error {
	_, err := uploader(&s3manager.UploadInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(file),
		Body:   bytes.NewReader(confirmationFile),
	})
	return err
}

func deleteS3File(bucket string, file string) error {
	sess, err := createSession()
	if err != nil {
		return err
	}
	svc := s3.New(sess)
	_, err = svc.DeleteObject(&s3.DeleteObjectInput{Bucket: aws.String(bucket), Key: aws.String(file)})
	if err != nil {
		log.Errorf("Unable to delete object: %v", err)
		return err
	}

	err = svc.WaitUntilObjectNotExists(&s3.HeadObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(file),
	})
	if err != nil {
		log.Warningf("Error occurred while waiting for object %q to be deleted, %v", file, err)
		return err
	}

	log.Printf("Object %q successfully deleted\n", file)
	return nil
}
