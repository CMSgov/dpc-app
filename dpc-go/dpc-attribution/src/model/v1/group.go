package v1

// Group is a struct that models the v1 Rosters table
type Group struct {
	ID             string `db:"id" json:"id" faker:"uuid_hyphenated"`
	ProviderID     string `db:"provider_id" json:"provider_id" faker:"uuid_hyphenated"`
	OrganizationID string `db:"organization_id" json:"organization_id" faker:"uuid_hyphenated"`
}

// GroupNPIs is a struct that models the NPIs for a given group
type GroupNPIs struct {
	OrgNPI      string `faker:"uuid_hyphenated"`
	ProviderNPI string `faker:"uuid_hyphenated"`
}
