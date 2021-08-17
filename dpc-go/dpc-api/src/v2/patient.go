package v2

import (
	"bufio"
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/constants"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/model"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"go.uber.org/zap"
	"net/http"
	"os"
	"time"
)

// PatientController is a struct that defines what the controller has
type PatientController struct {
	jc client.JobClient
}

// NewPatientController function that creates a organization controller and returns it's reference
func NewPatientController(jc client.JobClient) *PatientController {
	return &PatientController{
		jc,
	}
}

func (pc *PatientController) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	mbi, _ := r.Context().Value(constants.ContextKeyMBI).(string)

	jobID, err := pc.startExport(r, mbi)
	if err != nil {
		log.Error("Failed to start export", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "failed to retrieve patient data")
		return
	}

	batches, err := pc.waitForJobBatches(r.Context(), jobID)
	if err != nil {
		log.Error("Timed out waiting for job batches to finish", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "failed to retrieve patient data")
		return
	}

	bf := make([]model.BatchFile, 0)
	for _, b := range batches {
		for _, f := range *b.Files {
			bf = append(bf, f)
		}
	}

	var resp []byte
	var assembleErr error
	if len(bf) == 1 && bf[0].ResourceType == "OperationOutcome" {
		resp, assembleErr = assembleOperationOutcome(bf)
	} else {
		resp, assembleErr = assembleBundle(bf)
	}

	if assembleErr != nil {
		log.Error("Failed to assemble file data into response", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "failed to retrieve patient data")
	}
}

func assembleOperationOutcome(files []model.BatchFile) ([]byte, error) {
	exportPath := conf.GetAsString("exportPath")

	oo := fhir.OperationOutcome{}
	for _, f := range files {
		file, err := os.Open(fmt.Sprintf("%s/%s.ndjson", exportPath, f.FileName))
		if err != nil {
			return nil, err
		}

		scanner := bufio.NewScanner(file)
		scanner.Split(bufio.ScanLines)
		var text []string

		for scanner.Scan() {
			text = append(text, scanner.Text())
		}
		_ = file.Close()

		for _, line := range text {
			o, _ := fhir.UnmarshalOperationOutcome([]byte(line))
			oo.Issue = append(oo.Issue, o.Issue...)
		}
	}
	return oo.MarshalJSON()
}

func assembleBundle(files []model.BatchFile) ([]byte, error) {
	exportPath := conf.GetAsString("exportPath")
	entries := make([]fhir.BundleEntry, 0)
	for _, f := range files {
		file, err := os.Open(fmt.Sprintf("%s/%s.ndjson", exportPath, f.FileName))
		if err != nil {
			return nil, err
		}

		scanner := bufio.NewScanner(file)
		scanner.Split(bufio.ScanLines)
		var text []string

		for scanner.Scan() {
			text = append(text, scanner.Text())
		}
		_ = file.Close()

		for _, line := range text {
			entries = append(entries, fhir.BundleEntry{
				Resource: json.RawMessage(line),
			})
		}
	}

	return fhir.Bundle{
		Type:  fhir.BundleTypeSearchset,
		Entry: entries,
	}.MarshalJSON()
}

func (pc *PatientController) waitForJobBatches(ctx context.Context, jobID string) ([]model.BatchAndFiles, error) {
	jobWait := conf.GetAsInt("jobTimeoutInSeconds", 30)
	timeout := time.After(time.Duration(jobWait) * time.Second)
	for {
		select {
		case <-timeout:
			return nil, errors.New("timed out")
		default:
			bf := pc.getJobBatches(ctx, jobID)
			if bf != nil {
				return bf, nil
			}
			time.Sleep(250 * time.Millisecond)
		}
	}
}

func (pc *PatientController) getJobBatches(ctx context.Context, jobID string) []model.BatchAndFiles {
	log := logger.WithContext(ctx)
	b, err := pc.jc.Status(ctx, jobID)
	if err != nil {
		log.Warn("failed to get status")
		return nil
	}

	var batches []model.BatchAndFiles
	if err := json.Unmarshal(b, &batches); err != nil {
		log.Warn("failed to unmarshal to batches")
		return nil
	}

	statuses := GetStatus(batches)
	if (len(statuses) == 1 && statuses["COMPLETED"]) || statuses["FAILED"] {
		return batches
	}
	return nil
}

func (pc *PatientController) startExport(r *http.Request, mbi string) (string, error) {
	exportRequest := CreateExportRequest(r, "", []model.Attribution{
		{
			PatientMBI: mbi,
		},
	})
	jobID, err := pc.jc.Export(r.Context(), exportRequest)
	if err != nil {
		return "", err
	}
	return string(jobID), nil
}
