package middleware

type ContextKey int

const (
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization ContextKey = iota
	// ContextKeyGroup is the key in the context to retrieve the groupID
	ContextKeyGroup
	// ContextKeyRequestingIP is the key in the context to retrieve the requesting IP address
	ContextKeyRequestingIP
)
