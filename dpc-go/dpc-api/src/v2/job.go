package v2

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"sort"
	"time"

	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/model"
	"go.uber.org/zap"
)

// JobControllerImpl is a struct that defines what the controller has
type JobControllerImpl struct {
	jc client.JobClient
}

// NewJobController function that creates a organization controller and returns it's reference
func NewJobController(jc client.JobClient) JobController {
	return &JobControllerImpl{
		jc,
	}
}

// Status function that gets the job status according to FHIR Bulk Data
func (jc *JobControllerImpl) Status(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	jobID, ok := r.Context().Value(middleware.ContextKeyJobID).(string)
	if !ok {
		log.Error("Failed to extract the job id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract job id from url, please check the url")
		return
	}

	b, err := jc.jc.Status(r.Context(), jobID)
	if err != nil {
		log.Error("Failed to get the job status", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to get job status")
		return
	}

	var batches []model.BatchAndFiles
	if err := json.Unmarshal(b, &batches); err != nil {
		log.Error("Failed to unmarshal job data", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to get job status")
		return
	}

	statuses := getStatus(batches)
	if statuses["FAILED"] {
		log.Error(fmt.Sprintf("Failed batches found in job %s", jobID))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	} else if statuses["RUNNING"] || statuses["QUEUED"] {
		inProgress(w, batches)
		return
	} else if len(statuses) == 1 && statuses["COMPLETED"] {
		complete(r.Context(), w, batches)
		return
	} else {
		w.WriteHeader(http.StatusAccepted)
		return
	}

}

func complete(ctx context.Context, w http.ResponseWriter, batches []model.BatchAndFiles) {
	latestCompleteTime := getLatestCompleteTime(batches)
	if latestCompleteTime.Before(time.Now().Add(-time.Duration(24) * time.Hour)) {
		w.WriteHeader(http.StatusGone)
		return
	}

	files := make([]model.BatchFile, 0)
	for _, b := range batches {
		files = append(files, *b.Files...)
	}

	outputs, errors := formOutputList(files)

	jobExtension := make(map[string]interface{})
	jobExtension["https://dpc.cms.gov/submit_time"] = getEarliestSubmitTime(batches)
	jobExtension["https://dpc.cms.gov/complete_time"] = latestCompleteTime

	status := &model.Status{
		TransactionTime:     batches[0].Batch.TransactionTime,
		Request:             batches[0].Batch.RequestURL,
		RequiresAccessToken: true,
		Output:              outputs,
		Error:               errors,
		Extension:           jobExtension,
	}

	b, err := json.Marshal(status)
	if err != nil {
		fhirror.GenericServerIssue(ctx, w)
		return
	}

	w.Header().Add("Expires", latestCompleteTime.Add(time.Duration(24)*time.Hour).Format(time.RFC1123))
	if _, err := w.Write(b); err != nil {
		fhirror.GenericServerIssue(ctx, w)
		return
	}
}

func getEarliestSubmitTime(batches []model.BatchAndFiles) time.Time {
	sort.Slice(batches, func(i, j int) bool { return batches[i].Batch.SubmitTime.Before(batches[j].Batch.SubmitTime) })
	return batches[0].Batch.SubmitTime
}

func getLatestCompleteTime(batches []model.BatchAndFiles) time.Time {
	sort.Slice(batches, func(i, j int) bool { return batches[i].Batch.CompleteTime.After(*batches[j].Batch.CompleteTime) })
	return *batches[0].Batch.CompleteTime
}

func formOutputList(files []model.BatchFile) ([]model.Output, []model.Output) {
	var outputs = make([]model.Output, 0)
	var errors = make([]model.Output, 0)
	for _, f := range files {
		resourceType := f.ResourceType
		output := model.Output{
			Type: resourceType,
			URL:  fmt.Sprintf("%s/Data/%s.ndjson", conf.GetAsString("apiPath", ""), f.FormOutputFileName()),
		}
		if resourceType == "OperationOutcome" {
			errors = append(errors, output)
		} else {
			output.Count = f.Count
			output.Extension = fhirExtensions(f)
			outputs = append(outputs, output)
		}
	}
	return outputs, errors
}

func fhirExtensions(f model.BatchFile) map[string]interface{} {
	m := make(map[string]interface{})
	m["https://dpc.cms.gov/checksum"] = f.Checksum
	m["https://dpc.cms.gov/file_length"] = f.FileLength

	return m
}

func inProgress(w http.ResponseWriter, batches []model.BatchAndFiles) {
	progress := "QUEUED: 0.00%"
	patientsProcessed := 0
	totalPatients := 0
	for _, b := range batches {
		patientsProcessed += b.Batch.PatientsProcessed
		totalPatients += b.Batch.TotalPatients
	}
	if totalPatients > 0 {
		progress = fmt.Sprintf("RUNNING: %.2f%%", float32(patientsProcessed)/float32(totalPatients)*100.0)
	}
	w.Header().Add("X-Progress", progress)
	w.WriteHeader(202)
}

func getStatus(batches []model.BatchAndFiles) map[string]bool {
	statuses := make(map[string]bool)
	for _, b := range batches {
		statuses[b.Batch.Status] = true
	}
	return statuses
}
