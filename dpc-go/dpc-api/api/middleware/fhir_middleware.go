package middleware

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/model"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"net/http"
)

var log *zap.Logger

type ResponseWriter struct {
	http.ResponseWriter
	buf    *bytes.Buffer
	Status int
}

func (rw *ResponseWriter) Write(p []byte) (int, error) {
	return rw.buf.Write(p)
}

func (rw *ResponseWriter) WriteHeader(status int) {
	rw.Status = status
	rw.ResponseWriter.WriteHeader(status)
}

func FHIRModel(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log = logger.WithContext(r.Context())
		rw := &ResponseWriter{
			ResponseWriter: w,
			buf:            &bytes.Buffer{},
			Status:         200,
		}

		next.ServeHTTP(rw, r)

		var b = rw.buf.Bytes()
		if isSuccess(rw.Status) {
			body, err := convertToFHIR(b)
			if err != nil {
				log.Error(err.Error(), zap.Error(err))
				fhirror.GenericServerIssue(w, r.Context())
				return
			}
			b = body
		}
		if _, err := w.Write(b); err != nil {
			log.Error("Failed to write data", zap.Error(err))
			fhirror.GenericServerIssue(w, r.Context())
		}

	})
}

func convertToFHIR(body []byte) ([]byte, error) {
	var result model.Resource
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, err
	}

	fhirModel := result.Info
	if fhirModel == nil {
		return nil, errors.New("Malformed fhir model")
	}
	fhirModel["id"] = result.ID
	meta := make(map[string]string)
	meta["id"] = fmt.Sprintf("%s/%s", result.ResourceType(), result.ID)
	meta["versionId"] = result.VersionId()
	meta["lastUpdated"] = result.LastUpdated()
	fhirModel["meta"] = meta

	return json.Marshal(fhirModel)
}

func isSuccess(status int) bool {
	return status >= http.StatusOK && status < http.StatusMultipleChoices
}
