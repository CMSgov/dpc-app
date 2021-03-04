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
	"io/ioutil"
	"net/http"
)

type responseWriter struct {
	http.ResponseWriter
	buf    *bytes.Buffer
	Status int
}

// Write function to implement the http.ResponseWriter interface
func (rw *responseWriter) Write(p []byte) (int, error) {
	return rw.buf.Write(p)
}

// WriteHeader function that wraps the underlying http.ResponseWriter to hold the status
func (rw *responseWriter) WriteHeader(status int) {
	rw.Status = status
	rw.ResponseWriter.WriteHeader(status)
}

// FHIRFilter function that intercepts the request and modifies it before sending it further down the chain
func FHIRFilter(next http.Handler) http.Handler {
	fn := func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		defer func() {
			_ = r.Body.Close()
		}()
		body, err := ioutil.ReadAll(r.Body)
		if err != nil {
			log.Error("Failed to read the request body", zap.Error(err))
			fhirror.GenericServerIssue(r.Context(), w)
		}

		newBody, err := Filter(r.Context(), body)
		if err != nil {
			log.Error("Failed to filter request body", zap.Error(err))
			fhirror.GenericServerIssue(r.Context(), w)
		}
		r.Body = ioutil.NopCloser(bytes.NewBuffer(newBody))
		next.ServeHTTP(w, r)
	}
	return http.HandlerFunc(fn)
}

// FHIRModel function that intercepts the bytes being returned from the response
// if the response is successful, then the expected data is in the format of
// model.Resource where this will convert it into the appropriate FHIR object
func FHIRModel(next http.Handler) http.Handler {
	fn := func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		rw := &responseWriter{
			ResponseWriter: w,
			buf:            &bytes.Buffer{},
			Status:         200,
		}

		next.ServeHTTP(rw, r)
		b := rw.buf.Bytes()
		if isSuccess(rw.Status) {
			body, err := convertToFHIR(b)
			if err != nil {
				log.Error(err.Error(), zap.Error(err))
				fhirror.GenericServerIssue(r.Context(), w)
				return
			}
			b = body
		}
		if _, err := w.Write(b); err != nil {
			log.Error("Failed to write data", zap.Error(err))
			fhirror.GenericServerIssue(r.Context(), w)
		}
	}
	return http.HandlerFunc(fn)
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
	meta["versionId"] = result.VersionID()
	meta["lastUpdated"] = result.LastUpdated()
	fhirModel["meta"] = meta

	return json.Marshal(fhirModel)
}

func isSuccess(status int) bool {
	return status >= http.StatusOK && status < http.StatusMultipleChoices
}
