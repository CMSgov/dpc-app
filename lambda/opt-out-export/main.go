package main

import (
	"bytes"
	"context"
	"database/sql"
	"fmt"
	"os"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go/aws/session"
	_ "github.com/lib/pq"
	log "github.com/sirupsen/logrus"

	"opt-out-beneficiary-data-lambda/dpcaws"
)

type PatientInfo struct {
	beneficiary_id string
	first_name     sql.NullString
	last_name      sql.NullString
	dob            time.Time
	effective_date time.Time
	policy_code    sql.NullString
}

// Allow these to be switched out during unit tests
var getSecret = dpcaws.GetParameter
var getSecrets = dpcaws.GetParameters
var uploadToS3 = dpcaws.UploadFileToS3
var newLocalSession = dpcaws.NewLocalSession
var newSession = dpcaws.NewSession

var isTesting = os.Getenv("IS_TESTING") == "true"

func main() {
	if isTesting {
		var filename, err = generateBeneAlignmentFile()
		if err != nil {
			log.Error(err)
		} else {		
			log.Println(filename)
		}
	} else {
		lambda.Start(handler)
	}
}

func handler(ctx context.Context, event events.S3Event) (string, error) {
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})
	var filename, err = generateBeneAlignmentFile()
	if err != nil {
		return "", err
	}
	log.Info("Successfully completed executing export lambda")
	return filename, nil
}

func generateBeneAlignmentFile() (string, error) {
	session, sessErr := getAwsSession()
	if sessErr != nil {
		return "", sessErr
	}

	patientInfos := make(map[string]PatientInfo)

	attributionDbUser := fmt.Sprintf("/dpc/%s/attribution/db_read_only_user_dpc_attribution", os.Getenv("ENV"))
	attributionDbPassword := fmt.Sprintf("/dpc/%s/attribution/db_read_only_pass_dpc_attribution", os.Getenv("ENV"))
	consentDbUser := fmt.Sprintf("/dpc/%s/consent/db_read_only_user_dpc_consent", os.Getenv("ENV"))
	consentDbPassword := fmt.Sprintf("/dpc/%s/consent/db_read_only_pass_dpc_consent", os.Getenv("ENV"))
	var keynames []*string = make([]*string, 4)
	keynames[0] = &attributionDbUser
	keynames[1] = &attributionDbPassword
	keynames[2] = &consentDbUser
	keynames[3] = &consentDbPassword

	secretsInfo, pmErr := getSecrets(session, keynames)
	if pmErr != nil {
		return "", pmErr
	}

	attributionDbErr := getAttributionData(secretsInfo[attributionDbUser], secretsInfo[attributionDbPassword], patientInfos)
	if attributionDbErr != nil {
		return "", attributionDbErr
	}

	consentDbErr := getConsentData(secretsInfo[consentDbUser], secretsInfo[consentDbPassword], patientInfos)
	if consentDbErr != nil {
		return "", consentDbErr
	}

	fileName := generateAlignmentFileName(time.Now())
	buff, fileErr := formatFileData(fileName, patientInfos)

	if fileErr != nil {
		return "", fileErr
	}

	bfdSession, err := getAssumeRoleSession(session)
	if err != nil {
		return "", err
	}

	s3Err := uploadToS3(bfdSession, fileName, buff, os.Getenv("S3_UPLOAD_BUCKET"), os.Getenv("S3_UPLOAD_PATH"))
	if s3Err != nil {
		return "", s3Err
	}

	return fileName, nil
}

func formatFileData(fileName string, patientInfos map[string]PatientInfo) (bytes.Buffer, error) {
	var buff bytes.Buffer

	recordCount := 0
	curr_date := time.Now().Format("20060102")
	_, err := buff.WriteString(fmt.Sprintf("HDR_BENEDATAREQ%s\n", curr_date))
	if err != nil {
		log.Warning(fmt.Sprintf("Error writing header to file: %s", err))
		return buff, err
	}
	for _, patientInfo := range patientInfos {
		benePadded := fmt.Sprintf("%-*s", 11, patientInfo.beneficiary_id)
		fNamePadded := fmt.Sprintf("%-*s", 30, patientInfo.first_name.String)
		lNamePadded := fmt.Sprintf("%-*s", 40, patientInfo.last_name.String)
		dob := patientInfo.dob.Format("20060102")
		if patientInfo.dob.IsZero() {
			dob = " "
		}
		dobPadded := fmt.Sprintf("%-*s", 8, dob)
		effectiveDt := patientInfo.effective_date.Format("20060102")
		if patientInfo.effective_date.IsZero() {
			effectiveDt = " "
		}
		effectiveDtPadded := fmt.Sprintf("%-*s", 8, effectiveDt)
		optOutIndicator := ""
		if patientInfo.policy_code.Valid {
			if patientInfo.policy_code.String == "OPTOUT" {
				optOutIndicator = "N"
			} else {
				optOutIndicator = "Y"
			}
		}
		optOutIndicatorPadded := fmt.Sprintf("%-*s\n", 1, optOutIndicator)

		_, err = buff.WriteString(benePadded + fNamePadded + lNamePadded + dobPadded + effectiveDtPadded + optOutIndicatorPadded)

		if err != nil {
			log.Warning(fmt.Sprintf("Error writing to file: %s", err))
			return buff, err
		}
		recordCount += 1
	}
	buff.WriteString(fmt.Sprintf("TRL_BENEDATAREQ%s%010d", curr_date, recordCount))
	log.WithField("num_patients", len(patientInfos)).Info(fmt.Sprintf("Successfully generated beneficiary alignment file for file: %s", fileName))
	return buff, nil
}

func generateAlignmentFileName(now time.Time) string {
	fileFormat := "P#EFT.ON.DPC.NGD.REQ.D%s.T%s"

	date := now.Format("060102")
	time := now.Format("1504050")

	return fmt.Sprintf(fileFormat, date, time)
}

func getAssumeRoleSession(session *session.Session) (*session.Session, error) {
	parameterName := fmt.Sprintf("/opt-out-import/dpc/%s/bfd-bucket-role-arn", os.Getenv("ENV"))
	assumeRoleArn, err := getSecret(session, parameterName)

	if err != nil {
		return nil, fmt.Errorf("Failed to retrieve assume role arn: %w", err)
	}

	return newSession(assumeRoleArn)
}

func getAwsSession() (*session.Session, error) {
	// If we're testing, connect to local stack.  If we're not, connect to the AWS environment.
	if isTesting {
		endPoint, found := os.LookupEnv("LOCAL_STACK_ENDPOINT")
		if !found {
			return nil, fmt.Errorf("LOCAL_STACK_ENDPOINT env variable not defined")
		}
		return newLocalSession(endPoint)
	} else {
		return newSession("")
	}
}
