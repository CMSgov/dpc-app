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
	// PatientString is the string constant for Patient
	PatientString string = "Patient"
	// EoBString is the string constant for ExplanationOfBenefit
	EoBString string = "ExplanationOfBenefit"
	// CoverageString is the string constant for Coverage
	CoverageString string = "Coverage"
	// AllResources is the list of allowed resources for export
	AllResources string = "Patient,Coverage,ExplanationOfBenefit"
	// SinceLayout is the time format for the since parameter
	SinceLayout string = "2006-01-02T15:04:05-07:00"
	// Ndjson is an allowed output format strings for export requests
	Ndjson string = "ndjson"
	// ProvenanceHeader is the provenance header
	ProvenanceHeader string = "X-Provenance"
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization contextKey = iota
	// ContextKeyRequestURL is the key in the context to retrieve the requestURL
	ContextKeyRequestURL
	// ContextKeyGroup is the key in the context to retrieve the groupID
	ContextKeyGroup
	// ContextKeyImplementer is the key in the context to retrieve the ImplementerID
	ContextKeyImplementer
	// ContextKeyRequestingIP is the key in the context to retrieve the requesting IP address
	ContextKeyRequestingIP
	// ContextKeyFileName is the key in the context to retrieve the file name
	ContextKeyFileName
	// ContextKeyJobID is the key in the context to retrieve the jobID
	ContextKeyJobID
	// ContextKeyResourceTypes is the key in the context to pass on the _types param values
	ContextKeyResourceTypes
	// ContextKeySince is the key in the context to pass on the _since param value
	ContextKeySince
	// ContextKeyImplementor is the key in the context to pass on the implementor path param value
	ContextKeyImplementor
	// ContextKeyTokenID is the key in the context to pass on the tokenID param value
	ContextKeyTokenID
	// ContextKeyKeyID is the key in the context to pass on the keyID param value
	ContextKeyKeyID
	// ContextKeyProvenanceHeader is the key in the context to pass on the provenance header
	ContextKeyProvenanceHeader
)
