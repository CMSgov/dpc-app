package v1

// GroupNPIs is a struct that models the NPIs for a given group
type GroupNPIs struct {
	OrgNPI      string `faker:"uuid_hyphenated"`
	ProviderNPI string `faker:"uuid_hyphenated"`
}
