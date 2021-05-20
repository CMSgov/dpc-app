package middleware

type contextKey int

const (
	// OrgHeader is used in place of a auth token until SSAS is implemented
	OrgHeader string = "X-ORG"
	// FwdHeader is used to pass on the requesting IP address to attribution
	FwdHeader string = "X-Forwarded-For"
	// RequestURLHeader is used to pass on the requestingUrl to attribution
	RequestURLHeader string = "X-Request-Url"
	// FhirNdjson is an allowed output format strings for export requests
	FhirNdjson string = "application/fhir+ndjson"
	// ApplicationNdjson is an allowed output format strings for export requests
	ApplicationNdjson string = "application/ndjson"
	// Ndjson is an allowed output format strings for export requests
	Ndjson string = "ndjson"
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization contextKey = iota
	// ContextKeyRequestURL is the key in the context to retrieve the requestURL
	ContextKeyRequestURL
	// ContextKeyGroup is the key in the context to retrieve the groupID
	ContextKeyGroup
	// ContextKeyRequestingIP is the key in the context to retrieve the requesting IP address
	ContextKeyRequestingIP
	// ContextKeyFileName is the key in the context to retrieve the file name
	ContextKeyFileName
)
