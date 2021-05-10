package middleware

type ContextKey int

const (
	// OrgHeader is used in place of a auth token until SSAS is implemented
	OrgHeader string = "X-ORG"
	// FwdHeader is used to pass on the requesting IP address to attribution
	FwdHeader string = "X-Forwarded-For"
	// FHIR_JSON is an allowed output format strings for export requests
	FHIR_JSON string = "application/fhir+json"
	// FHIR_NDJSON is an allowed output format strings for export requests
	FHIR_NDJSON string = "application/fhir+ndjson"
	// APPLICATION_NDJSON is an allowed output format strings for export requests
	APPLICATION_NDJSON string = "application/ndjson"
	// NDJSON is an allowed output format strings for export requests
	NDJSON string = "ndjson"
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization ContextKey = iota
	// ContextKeyGroup is the key in the context to retrieve the groupID
	ContextKeyGroup
	// ContextKeyRequestingIP is the key in the context to retrieve the requesting IP address
	ContextKeyRequestingIP
)
