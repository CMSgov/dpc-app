package pkg

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/pkg/model"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
	"net/http"
)

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

func FHIRMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		rw := &ResponseWriter{
			ResponseWriter: w,
			buf:            &bytes.Buffer{},
			Status:         200,
		}

		next.ServeHTTP(rw, r)

		var b = rw.buf.Bytes()
		if isSuccess(rw.Status) {
			body, err := convertToFHIR(b)
			b = body
			if err != nil {
				zap.L().Error("Failed to convert to fhir", zap.Error(err))
				boom.Internal(w, err.Error())
			}
		}

		if _, err := w.Write(b); err != nil {
			zap.L().Error("Failed to write data", zap.Error(err))
			boom.Internal(w, err.Error())
		}

	})
}

func convertToFHIR(body []byte) ([]byte, error) {
	var result model.Resource
	if err := json.Unmarshal(body, &result); err != nil {
		zap.L().Error("Failed to convert to FHIR model", zap.Error(err))
		return nil, err
	}

	fhirModel := result.Info
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
