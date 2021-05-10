package middleware

type contextKey int

// OrgHeader is used in place of a auth token until SSAS is implemented
const OrgHeader string = "X-ORG"

const (
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization contextKey = iota
	// ContextKeyGroup is the key in the context to retrieve the groupID
	ContextKeyGroup
	// ContextKeyRequestingIP is the key in the context to retrieve the requesting IP address
	ContextKeyRequestingIP
)
