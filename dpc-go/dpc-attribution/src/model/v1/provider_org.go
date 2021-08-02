package v1

// ProviderOrg is a struc that defines the provider/org relationship
type ProviderOrg struct {
	OrgNPI      string `faker:"uuid_hyphenated"`
	ProviderNPI string `faker:"uuid_hyphenated"`
}
