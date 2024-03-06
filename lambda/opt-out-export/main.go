package main

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"os"
	"time"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/ssm"
	_ "github.com/lib/pq"
	log "github.com/sirupsen/logrus"
)

type Event struct {
	date string `json:"date"`
}

type PatientInfo struct {
	beneficiary_id string
	first_name     sql.NullString
	last_name      sql.NullString
	dob            time.Time
	effective_date time.Time
	policy_code    sql.NullString
}

var isTesting = os.Getenv("IS_TESTING") == "true"

func main() {
	if isTesting {
		var filename, _ = generateBeneAlignmentFile()
		log.Println(filename)
	} else {
		lambda.Start(handler)
	}
}

func handler(ctx context.Context, event Event) (string, error) {
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

	secretsInfo, pmErr := getSecrets(keynames)
	if pmErr != nil {
		if isTesting {
			secretsInfo = make(map[string]string)
			secretsInfo[attributionDbUser] = os.Getenv("DPC_DB_USER")
			secretsInfo[attributionDbPassword] = os.Getenv("DPC_DB_PASS")
			secretsInfo[consentDbUser] = os.Getenv("DPC_DB_USER")
			secretsInfo[consentDbPassword] = os.Getenv("DPC_DB_PASS")
		} else {
			return "", pmErr
		}
	}

	attributionDbErr := getAttributionData(secretsInfo[attributionDbUser], secretsInfo[attributionDbPassword], patientInfos)
	if attributionDbErr != nil {
		return "", attributionDbErr
	}

	consentDbErr := getConsentData(secretsInfo[consentDbUser], secretsInfo[consentDbPassword], patientInfos)
	if consentDbErr != nil {
		return "", consentDbErr
	}

	filename, fileErr := formatFileData(patientInfos)
	if fileErr != nil {
		return "", fileErr
	}

	return filename, nil
}

var getSecrets = func(keynames []*string) (map[string]string, error) {
	sess, err := session.NewSession(&aws.Config{
		Region: aws.String("us-east-1"),
	})
	if err != nil {
		log.Warning(fmt.Sprintf("Error creating session: %s", err))
		return nil, err
	}
	ssmsvc := ssm.New(sess)

	withDecryption := true
	param, err := ssmsvc.GetParameters(&ssm.GetParametersInput{
		Names:          keynames,
		WithDecryption: &withDecryption,
	})
	if err != nil {
		log.Warning(fmt.Sprintf("Error connecting to parameter store: %s", err))
		return nil, err
	}
	if len(param.InvalidParameters) > 0 {
		invalidParamsStr := ""
		for i := 0; i < len(param.InvalidParameters); i++ {
			invalidParamsStr += fmt.Sprintf("%s,\n", *param.InvalidParameters[i])
		}
		err_msg := fmt.Sprintf("Invalid parameters error: %s", invalidParamsStr)
		log.Warning(err_msg)
		return nil, errors.New(err_msg)
	}

	var secretsInfo map[string]string = make(map[string]string)

	for _, item := range param.Parameters {
		secretsInfo[*item.Name] = *item.Value
	}
	return secretsInfo, nil
}

func formatFileData(patientInfos map[string]PatientInfo) (string, error) {
	filename := "bene_alignment_file.txt"

	file, err := os.Create(filename)
	if err != nil {
		log.Warning(fmt.Sprintf("Error creating file: %s", err))
		return "", err
	}
	defer file.Close()

	recordCount := 0
	curr_date := time.Now().Format("20060102")
	_, err = file.WriteString(fmt.Sprintf("HDR_BENEDATAREQ%s\n", curr_date))
	if err != nil {
		log.Warning(fmt.Sprintf("Error writing header to file: %s", err))
		return "", err
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

		_, err = file.WriteString(benePadded + fNamePadded + lNamePadded + dobPadded + effectiveDtPadded + optOutIndicatorPadded)

		if err != nil {
			log.Warning(fmt.Sprintf("Error writing to file: %s", err))
			return "", err
		}
		recordCount += 1
	}
	file.WriteString(fmt.Sprintf("TRL_BENEDATAREQ%s%010d", curr_date, recordCount))
	log.WithField("num_patients", len(patientInfos)).Info(fmt.Sprintf("Successfully generated beneficiary alignment file: %s", filename))
	return filename, nil
}
