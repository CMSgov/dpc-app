package v1

//ExportRequest struct to hold data for export request
type ExportRequest struct {
	GroupID      string   `json:"groupID"`
	OutputFormat string   `json:"outputFormat"`
	Since        string   `json:"since"`
	Type         string   `json:"type"`
	MBIs         []string `json:"mbis"`
	ProviderNPI  string   `json:"provider"`
}
