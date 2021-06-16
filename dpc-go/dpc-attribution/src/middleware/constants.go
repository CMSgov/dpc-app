package middleware

// ContextKey is exported for use in helper method `FetchValueFromContext`
type ContextKey int

const (
	// OrgHeader is used in place of a auth token until SSAS is implemented
	OrgHeader string = "X-ORG"
	// FwdHeader is used to pass on the requesting IP address to attribution
	FwdHeader string = "X-Forwarded-For"
	// RequestURLHeader is used to pass on the requestingUrl to attribution
	RequestURLHeader string = "X-Request-Url"
	// SinceLayout is the time format for the since parameter
	SinceLayout string = "2006-01-02T15:04:05-07:00"
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization ContextKey = iota
	// ContextKeyGroup is the key in the context to retrieve the groupID
	ContextKeyGroup
	// ContextKeyFileName is the key in the context to retrieve the file name
	ContextKeyFileName
	// ContextKeyImplementer is the key in the context to retrieve the ImplementerID
	ContextKeyImplementer
	// ContextKeyJobID is the key in the context to retrieve the JobID
	ContextKeyJobID
)
