package middleware

import (
	"bytes"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"html"
	"io/ioutil"
	"net/http"
	"strings"

	"github.com/microcosm-cc/bluemonday"
)

func Sanitize(next http.Handler) http.Handler {
	fn := func(w http.ResponseWriter, r *http.Request) {
		if r.Body == http.NoBody {
			next.ServeHTTP(w, r)
			return
		}

		log := logger.WithContext(r.Context())
		defer func() {
			if err := r.Body.Close(); err != nil {
				log.Error("Failed to close request body", zap.Error(err))
				fhirror.GenericServerIssue(r.Context(), w)
			}
		}()
		ogb, err := ioutil.ReadAll(r.Body)
		if err != nil {
			log.Error("Failed to read request body", zap.Error(err))
			fhirror.GenericServerIssue(r.Context(), w)
		}
		p := bluemonday.StrictPolicy()
		b := p.SanitizeBytes(ogb)
		s := html.UnescapeString(string(b))
		if !bytes.Equal(ogb, []byte(s)) {
			log.Error("xss detected in body")
			fhirror.GenericServerIssue(r.Context(), w)
			return
		}
		r.Body = ioutil.NopCloser(strings.NewReader(s))
		next.ServeHTTP(w, r)
	}
	return http.HandlerFunc(fn)
}
