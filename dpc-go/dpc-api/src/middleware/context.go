package middleware

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/constants"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"github.com/sjsdfg/common-lang-in-go/StringUtils"
	"go.uber.org/zap"
	"net/http"
	"regexp"
	"strings"
	"time"

	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/go-chi/chi"
)

// AdminOrganizationCtx middleware to extract the organizationID from the chi url param and set it into the request context
func AdminOrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), constants.ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// OrganizationCtx middleware to extract the organizationID from the chi url param and set it into the request context
func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authOrgID := r.Context().Value(constants.ContextKeyOrganization).(string)
		organizationID := chi.URLParam(r, "organizationID")
		if !StringUtils.EqualIgnoreCase(authOrgID, organizationID) {
			fhirror.BusinessViolation(r.Context(), w, http.StatusUnauthorized, "Not Allowed")
			return
		}
		ctx := context.WithValue(r.Context(), constants.ContextKeyOrganization, authOrgID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AuthCtx middleware gets the organization ID from the access token
func AuthCtx(ssasClient client.SsasClient) func(next http.Handler) http.Handler {
	// Return context with organizationID
	return func(next http.Handler) http.Handler {
		//return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fn := func(w http.ResponseWriter, r *http.Request) {
			log := logger.WithContext(r.Context())

			// Extract access token from authorization header:
			reqToken := r.Header.Get("Authorization")
			if len(reqToken) <= 0 {
				log.Error("Missing access token")
				fhirror.ServerIssue(r.Context(), w, http.StatusForbidden, "Missing access token")
				return
			}
			bearerTokenParts := strings.Split(reqToken, "Bearer ")
			bearerToken := bearerTokenParts[1]
			if len(bearerToken) <= 0 {
				log.Error("Missing access token")
				fhirror.ServerIssue(r.Context(), w, http.StatusForbidden, "Missing access token")
				return
			}

			orgID, err := ssasClient.ValidateAccessToken(r.Context(), bearerToken)

			if err != nil {
				log.Error("Invalid access token", zap.Error(err))
				fhirror.ServerIssue(r.Context(), w, http.StatusForbidden, "Invalid access token")
				return
			}

			ctx := context.WithValue(r.Context(), constants.ContextKeyOrganization, orgID)
			next.ServeHTTP(w, r.WithContext(ctx))
		}

		return http.HandlerFunc(fn)
	}
}

// GroupCtx middleware to extract the groupID from the chi url param and set it into the request context
func GroupCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "groupID")
		ctx := context.WithValue(r.Context(), constants.ContextKeyGroup, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// ImplementerCtx middleware to extract the ImplementerID from the chi url param and set it into the request context
func ImplementerCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ImplementerID := chi.URLParam(r, "implementerID")
		ctx := context.WithValue(r.Context(), constants.ContextKeyImplementer, ImplementerID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// FileNameCtx middleware to extract the filename from the chi url param and set it into the request context
func FileNameCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "fileName")
		ctx := context.WithValue(r.Context(), constants.ContextKeyFileName, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// RequestIPCtx middleware to extract the requesting IP address from the incoming request and set it into the request context
func RequestIPCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ipAddress := r.Header.Get(constants.FwdHeader)
		if ipAddress == "" {
			ipAddress = r.RemoteAddr
		}
		ctx := context.WithValue(r.Context(), constants.ContextKeyRequestingIP, ipAddress)
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
		ctx := context.WithValue(r.Context(), constants.ContextKeyRequestURL, fmt.Sprintf("%s://%s%s", scheme, r.Host, r.RequestURI))
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// ExportTypesParamCtx middleware to extract the export _type param
func ExportTypesParamCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		types := r.URL.Query().Get("_type")
		if types == "" {
			types = constants.AllResources
		}
		if !validateTypes(types) {
			log.Error(fmt.Sprintf("Invalid resource type: %s", types))
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Invalid resource type")
			return
		}
		ctx := context.WithValue(r.Context(), constants.ContextKeyResourceTypes, types)
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
		constants.PatientString,
		constants.CoverageString,
		constants.EoBString:
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
		ctx := context.WithValue(r.Context(), constants.ContextKeySince, s)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func validateSince(since string) (string, string) {
	if since == "" {
		return "", ""
	}
	p, err := time.Parse(constants.SinceLayout, since)
	if err != nil {
		return "", "Could not parse _since"
	}
	if p.After(time.Now()) {
		return "", "_since cannot be a future date"
	}
	return p.Format(constants.SinceLayout), ""
}

// JobCtx middleware to extract the jobID from the chi url param and set it into the request context
func JobCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		jobID := chi.URLParam(r, "jobID")
		ctx := context.WithValue(r.Context(), constants.ContextKeyJobID, jobID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// TokenCtx middleware to extract the tokenID from the chi url param and set it into the request context
func TokenCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		tokenID := chi.URLParam(r, "tokenID")
		ctx := context.WithValue(r.Context(), constants.ContextKeyTokenID, tokenID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// PublicKeyCtx middleware to extract the keyID from the chi url param and set it into the request context
func PublicKeyCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		keyID := chi.URLParam(r, "keyID")
		ctx := context.WithValue(r.Context(), constants.ContextKeyKeyID, keyID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// ProvenanceHeaderValidator middleware to require and validate a provenance header
func ProvenanceHeaderValidator(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		provenanceHeaderStr := r.Header.Get(constants.ProvenanceHeader)
		if StringUtils.IsEmpty(provenanceHeaderStr) {
			log.Error("Provenance header is missing")
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Provenance header is required")
			return
		}
		ph, err := fhir.UnmarshalProvenance([]byte(provenanceHeaderStr))
		if err != nil {
			log.Error("Could not unmarshal provenance header", zap.Error(err))
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Could not parse provenance header")
			return
		}
		t, err := parseTime(ph.Recorded)
		if err != nil {
			log.Error("Could not extract recorded timestamp", zap.Error(err))
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Recorded timestamp invalid")
			return
		}

		provenanceHourDiff := conf.GetAsInt("provenance-hour-diff", 24)
		now := time.Now()
		diff := now.Sub(t)
		if diff.Hours() > float64(provenanceHourDiff) || t.After(now) {
			log.Error(fmt.Sprintf("Recorded timestamp invalid because it's outside the %d hr window", provenanceHourDiff))
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, fmt.Sprintf("Recorded timestamp invalid because it's outside the %d hr window", provenanceHourDiff))
			return
		}

		cc := ph.Reason
		if len(cc) != 1 || len(cc[0].Coding) != 1 ||
			!StringUtils.EqualIgnoreCase(StringUtils.GetPtrValueWithDefault(cc[0].Coding[0].System, ""), "http://hl7.org/fhir/v3/ActReason") ||
			!StringUtils.EqualIgnoreCase(StringUtils.GetPtrValueWithDefault(cc[0].Coding[0].Code, ""), "TREAT") {
			log.Error("Invalid reason")
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Invalid reason")
			return
		}

		ag := ph.Agent
		if len(ag) != 1 || len(ag[0].Role) != 1 || len(ag[0].Role[0].Coding) != 1 ||
			!StringUtils.EqualIgnoreCase(StringUtils.GetPtrValueWithDefault(ag[0].Role[0].Coding[0].System, ""), "http://hl7.org/fhir/v3/RoleClass") ||
			!StringUtils.EqualIgnoreCase(StringUtils.GetPtrValueWithDefault(ag[0].Role[0].Coding[0].Code, ""), "AGNT") {
			log.Error("Invalid role")
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Invalid role")
			return
		}

		regex, _ := regexp.Compile("Organization/(?P<org>[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})")
		if len(ag) != 1 || ag[0].Who.Reference == nil || !regex.MatchString(StringUtils.GetPtrValue(ag[0].Who.Reference)) {
			log.Error("Invalid who reference")
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Invalid who reference")
			return
		}

		authOrg, ok := r.Context().Value(constants.ContextKeyOrganization).(string)
		if !ok {
			log.Error("Auth org not found")
			fhirror.GenericServerIssue(r.Context(), w)
			return
		}

		provenanceOrg := reSubMatchMap(regex, *ag[0].Who.Reference)["org"]
		if !StringUtils.EqualIgnoreCase(authOrg, provenanceOrg) {
			log.Error("Auth org and provenance org don't match")
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Org in Provenance not valid")
			return
		}

		log.Info(fmt.Sprintf("Provenance header: %s", provenanceHeaderStr))
		ctx := context.WithValue(r.Context(), constants.ContextKeyProvenanceHeader, provenanceHeaderStr)
		next.ServeHTTP(w, r.WithContext(ctx))
	})

}

func MBICtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		system := r.Header.Get(constants.FHIRIdentifierSystemHeader)
		if !StringUtils.Equal(system, "http://hl7.org/fhir/sid/us-mbi") {
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "invalid mbi system identifier in header")
			return
		}
		mbi := r.Header.Get(constants.FHIRIdentifierValueHeader)
		if StringUtils.IsEmpty(mbi) {
			fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "invalid mbi value in header")
			return
		}
		ctx := context.WithValue(r.Context(), constants.ContextKeyMBI, mbi)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func reSubMatchMap(r *regexp.Regexp, str string) map[string]string {
	match := r.FindStringSubmatch(str)
	subMatchMap := make(map[string]string)
	for i, name := range r.SubexpNames() {
		if i != 0 {
			subMatchMap[name] = match[i]
		}
	}
	return subMatchMap
}

func parseTime(input string) (time.Time, error) {
	formats := []string{constants.SinceLayout, "2006-01-02T15:04:05Z"}
	for _, format := range formats {
		t, err := time.Parse(format, input)
		if err == nil {
			return t, nil
		}
	}
	return time.Time{}, errors.New("Unrecognized time format")
}
