package v1

type ExportRequest struct {
	GroupID      string   `json:"groupID"`
	OutputFormat string   `json:"outputFormat"`
	Since        string   `json:"since"`
	Type         string   `json:"type"`
	MBIs         []string `json:"mbis"`
	ProviderNPI  string   `json:"provider"`
}
