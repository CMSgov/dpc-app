package model

//ProxyPublicKeyRequest struct to hold data for public key request
type ProxyPublicKeyRequest struct {
	PublicKey string `json:"public_key"`
	Signature string `json:"signature"`
}

//ExportRequest struct to hold data for export request
type ExportRequest struct {
	GroupID      string   `json:"groupID"`
	OutputFormat string   `json:"outputFormat"`
	Since        string   `json:"since,omitempty"`
	Type         string   `json:"type"`
	MBIs         []string `json:"mbis"`
	ProviderNPI  string   `json:"provider"`
}
