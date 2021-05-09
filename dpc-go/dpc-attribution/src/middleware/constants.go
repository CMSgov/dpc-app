package middleware

// ContextKey is exported for use in helper method `FetchValueFromContext`
type ContextKey int

const (
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization ContextKey = iota
	// ContextKeyGroup is the key in the context to retrieve the groupID
	ContextKeyGroup
	// ContextKeyRequestingIP is the key in the context to retrieve the requesting IP address
	ContextKeyRequestingIP
	// ContextKeyFileName is the key in the context to retrieve the file name
	ContextKeyFileName
)
