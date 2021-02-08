package pkg

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/pkg/model"
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
		}

		next.ServeHTTP(rw, r)
		b := rw.buf.Bytes()

		if rw.Status == 200 {
			var result model.Resources
			if err := json.Unmarshal(b, &result); err != nil {
				zap.L().Error("Failed to convert to FHIR model", zap.Error(err))
				boom.Internal(w, err.Error())
				return
			}

			fhirModel := result.Info
			fhirModel["id"] = result.ID
			meta := make(map[string]string)
			meta["id"] = fmt.Sprintf("%s/%s", result.ResourceType(), result.ID)
			meta["versionId"] = result.VersionId()
			meta["lastUpdated"] = result.LastUpdated()
			fhirModel["meta"] = meta

			b, err := json.Marshal(fhirModel)
			if err != nil {
				zap.L().Error("Failed to convert to FHIR model", zap.Error(err))
				boom.Internal(w, err.Error())
				return
			}
			if _, err := w.Write(b); err != nil {
				zap.L().Error("Failed to write data", zap.Error(err))
				boom.Internal(w, err.Error())
			}
		} else {
			if _, err := w.Write(b); err != nil {
				zap.L().Error("Failed to write data", zap.Error(err))
				boom.Internal(w, err.Error())
			}
		}

	})
}
