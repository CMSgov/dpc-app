package model

type ManagedOrg struct {
    ID        string    `json:"id"`
    Name      string    `json:"name"`
    Npi       string    `json:"npi"`
    Status ImplOrgStatus `json:"status"`
}
