package middleware

import (
	"context"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/go-chi/chi"
)

// OrganizationCtx middleware to extract the organizationID from the chi url param and set it into the request context
func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AuthCtx middleware is placeholder to get org id from token until we do SSAS
func AuthCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		organizationID := r.Header.Get(OrgHeader)
		if organizationID == "" {
			log.Error("Missing auth token")
			fhirror.ServerIssue(r.Context(), w, http.StatusForbidden, "Missing auth token")
			return
		}
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GroupCtx middleware to extract the groupID from the chi url param and set it into the request context
func GroupCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "groupID")
		ctx := context.WithValue(r.Context(), ContextKeyGroup, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// ImplementerCtx middleware to extract the ImplementerID from the chi url param and set it into the request context
func ImplementerCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ImplementerID := chi.URLParam(r, "implementerID")
		ctx := context.WithValue(r.Context(), ContextKeyImplementer, ImplementerID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// FileNameCtx middleware to extract the filename from the chi url param and set it into the request context
func FileNameCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "fileName")
		ctx := context.WithValue(r.Context(), ContextKeyFileName, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// ImplementorIDCtx middleware to extract the implementor id from the chi url param and set it into the request context
func ImplementorIDCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := chi.URLParam(r, "implID")
		ctx := context.WithValue(r.Context(), ContextKeyImplementor, id)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// OrganizationIDCtx middleware to extract the organization id from the chi url param and set it into the request context
func OrganizationIDCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := chi.URLParam(r, "orgID")
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, id)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// RequestIPCtx middleware to extract the requesting IP address from the incoming request and set it into the request context
func RequestIPCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ipAddress := r.Header.Get(FwdHeader)
		if ipAddress == "" {
			ipAddress = r.RemoteAddr
		}
		ctx := context.WithValue(r.Context(), ContextKeyRequestingIP, ipAddress)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// RequestURLCtx middleware to extract the requesting URL from the incoming request and set it in the request context
func RequestURLCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		scheme := "http"
		if r.TLS != nil {
			scheme = "https"
		}
		ctx := context.WithValue(r.Context(), ContextKeyRequestURL, fmt.Sprintf("%s://%s%s", scheme, r.Host, r.RequestURI))
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// ExportTypesParamCtx middleware to extract the export _type param
func ExportTypesParamCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		types := r.URL.Query().Get("_type")
		if types == "" {
			types = AllResources
		}
		if !validateTypes(types) {
			log.Error(fmt.Sprintf("Invalid resource type: %s", types))
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Invalid resource type")
			return
		}
		ctx := context.WithValue(r.Context(), ContextKeyResourceTypes, types)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func validateTypes(types string) bool {
	t := strings.Split(types, ",")
	for _, s := range t {
		if !isValidType(s) {
			return false
		}
	}
	return true
}

func isValidType(t string) bool {
	switch t {
	case
		PatientString,
		CoverageString,
		EoBString:
		return true
	}
	return false
}

// ExportSinceParamCtx middleware to extract the export _since param
func ExportSinceParamCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		since := r.URL.Query().Get("_since")
		s, msg := validateSince(since)
		if msg != "" {
			log.Error(msg)
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, msg)
			return
		}
		ctx := context.WithValue(r.Context(), ContextKeySince, s)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func validateSince(since string) (string, string) {
	if since == "" {
		return "", ""
	}
	p, err := time.Parse(SinceLayout, since)
	if err != nil {
		return "", "Could not parse _since"
	}
	if p.After(time.Now()) {
		return "", "_since cannot be a future date"
	}
	return p.Format(SinceLayout), ""
}

// JobCtx middleware to extract the jobID from the chi url param and set it into the request context
func JobCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		jobID := chi.URLParam(r, "jobID")
		ctx := context.WithValue(r.Context(), ContextKeyJobID, jobID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// TokenCtx middleware to extract the tokenID from the chi url param and set it into the request context
func TokenCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		tokenID := chi.URLParam(r, "tokenID")
		ctx := context.WithValue(r.Context(), ContextKeyTokenID, tokenID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// PublicKeyCtx middleware to extract the keyID from the chi url param and set it into the request context
func PublicKeyCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		keyID := chi.URLParam(r, "keyID")
		ctx := context.WithValue(r.Context(), ContextKeyKeyID, keyID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
