package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"time"

	log "github.com/sirupsen/logrus"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	awsv2 "github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	stscredsv2 "github.com/aws/aws-sdk-go-v2/credentials/stscreds"
	"github.com/aws/aws-sdk-go-v2/feature/s3/manager"
	s3v2 "github.com/aws/aws-sdk-go-v2/service/s3"
	stsv2 "github.com/aws/aws-sdk-go-v2/service/sts"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials/stscreds"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"

	"github.com/ianlopshire/go-fixedwidth"
)

var isTesting = os.Getenv("IS_TESTING") == "true"

var createConnectionVar = createConnection

// Used for dependency injection.  Allows us to easily mock these function in unit tests.
type (
	// s3manager.NewUploader.upload
	S3Uploader func(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error)

	// fixedWidth.Marshal
	FileMarshaler func(v interface{}) ([]byte, error)
	// fixedWidth.Unmarshal
	FileUnmarshaler func(data []byte, v interface{}) error
)

func main() {
	if isTesting {
		filename := "bfdeft01/dpc/in/P.NGD.DPC.RSP.D240123.T1122001.IN"
		success, _ := importOptOutFile("demo-bucket", filename)
		log.Println(success)
	} else {
		lambda.Start(handler)
	}
}

func handler(ctx context.Context, sqsEvent events.SQSEvent) (string, error) {
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})

	log.Info(sqsEvent.Records[0].Body)

	for _, sqsRecord := range sqsEvent.Records {
		var snsEvent events.SNSEvent
		err := json.Unmarshal([]byte(sqsRecord.Body), &snsEvent)

		unmarshalTypeErr := new(json.UnmarshalTypeError)
		if errors.As(err, &unmarshalTypeErr) {
			log.Warn("Skipping event due to unrecognized format for SNS")
			continue
		}

		for _, snsRecord := range snsEvent.Records {
			var s3Event events.S3Event
			err := json.Unmarshal([]byte(snsRecord.SNS.Message), &s3Event)

			unmarshalTypeErr := new(json.UnmarshalTypeError)
			if errors.As(err, &unmarshalTypeErr) {
				log.Warn("Skipping event due to unrecognized format for S3")
				continue
			}

			for _, e := range s3Event.Records {
				if e.EventName == "ObjectCreated:Put" {
					success, err := importOptOutFile(e.S3.Bucket.Name, e.S3.Object.Key)
					log.Info(success)
					if err != nil {
						return e.S3.Object.Key, err
					}
					err = deleteS3File(e.S3.Bucket.Name, e.S3.Object.Key)
					return e.S3.Object.Key, err
				}
			}
		}
	}

	return "", nil
}

func importOptOutFile(bucket string, file string) (bool, error) {
	log.Info(fmt.Printf("Importing opt out file: %s (bucket: %s)", file, bucket))
	metadata, err := ParseMetadata(bucket, file)
	if err != nil {
		log.Warning(fmt.Sprintf("Failed to parse opt out file metadata: %s", err))
		return false, err
	}

	dbuser := fmt.Sprintf("/dpc/%s/consent/db_user_dpc_consent", os.Getenv("ENV"))
	dbpassword := fmt.Sprintf("/dpc/%s/consent/db_pass_dpc_consent", os.Getenv("ENV"))
	secrets, err := getConsentDbSecrets(dbuser, dbpassword)
	if err != nil {
		log.Warning(fmt.Sprintf("Failed to get DB secrets: %s", err))
		return false, err
	}

	db, err := createConnectionVar(secrets[dbuser], secrets[dbpassword])
	if err != nil {
		log.Warning(fmt.Sprintf("Failed to create db connection: %s", err))
		return false, err
	}
	if err := db.Ping(); err != nil {
		log.Warning(fmt.Sprintf("Ping error: %s", err))
		return false, err
	}
	defer db.Close()

	optOutFileEntity, err := insertOptOutMetadata(db, &metadata)
	if err != nil {
		log.Warning(fmt.Sprintf("Failed to insert opt out metadata: %s", err))
		return false, err
	}

	bytes, err := downloadS3File(bucket, file)
	if err != nil {
		log.Warningf("Failed to download opt out file from S3: %s", err)
		if err := updateOptOutFileImportStatus(db, optOutFileEntity.id, ImportFail); err != nil {
			return false, err
		}
		return false, err
	}

	records, err := ParseConsentRecords(&metadata, bytes)
	if err != nil {
		log.Warning(fmt.Sprintf("Failed to parse consent records: %s", err))
		if err := updateOptOutFileImportStatus(db, optOutFileEntity.id, ImportFail); err != nil {
			return false, err
		}
		return false, err
	}
	createdRecords, err := insertConsentRecords(db, optOutFileEntity.id, records)
	if err != nil {
		log.Warning(fmt.Sprintf("Failed to insert consent records: %s", err))
		return false, err
	}
	log.WithField("created_records_count", len(createdRecords)).Info("Created consent records with the following ID fields:")
	for _, rec := range createdRecords {
		log.Info(fmt.Sprintf("ID: %s", rec.ID))
	}

	confirmationFile, err := generateConfirmationFile(true, records, fixedwidth.Marshal)
	if err != nil {
		log.Warning(fmt.Sprintf("Failed to generate confirmation file: %s", err))
		return false, err
	}

	if sess, err := createSession(); err != nil {
		log.Warning("Failed to create session for uploading response file")
		return false, err
	} else {
		if err = uploadConfirmationFile(bucket, GenerateConfirmationFileName(file), s3manager.NewUploader(sess).Upload, confirmationFile); err != nil {
			log.Warning("Failed to write upload response file")
			return false, err
		}
	}

	return true, err
}

func createV2Cfg() (*awsv2.Config, error) {
	cfg, err := config.LoadDefaultConfig(context.TODO(), config.WithRegion("us-east-1"))
	assumeRoleArn, err := getAssumeRoleArn()

	if err != nil {
		return nil, err
	}

	client := stsv2.NewFromConfig(cfg)
	creds := stscredsv2.NewAssumeRoleProvider(client, assumeRoleArn)
	cfg.Credentials = awsv2.NewCredentialsCache(creds)
	return &cfg, nil
}

func createSession() (*session.Session, error) {
	sess := session.Must(session.NewSession())
	var err error
	if isTesting {
		sess, err = session.NewSessionWithOptions(session.Options{
			Profile: "default",
			Config: aws.Config{
				Region:           aws.String("us-east-1"),
				S3ForcePathStyle: aws.Bool(true),
				Endpoint:         aws.String("http://localhost:4566"),
			},
		})
	} else {
		assumeRoleArn, err := getAssumeRoleArn()

		if err == nil {
			sess, err = session.NewSession(&aws.Config{
				Region: aws.String("us-east-1"),
				Credentials: stscreds.NewCredentials(
					sess,
					assumeRoleArn,
				),
			})
		}
	}

	if err != nil {
		return nil, err
	}

	return sess, nil
}

func downloadS3File(bucket string, file string) ([]byte, error) {
	cfg, err := createV2Cfg()
	if err != nil {
		return []byte{}, err
	}

	downloader := manager.NewDownloader(s3v2.NewFromConfig(*cfg))
	buff := &aws.WriteAtBuffer{}
	numBytes, err := downloader.Download(context.TODO(), buff, &s3v2.GetObjectInput{
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
		TrailerCode:       "TLR_BENECONFIRM",
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
