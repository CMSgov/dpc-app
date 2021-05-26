package model

import (
	"fmt"
	"time"
)

type BatchAndFiles struct {
	Batch *BatchInfo   `json:"batch"`
	Files *[]BatchFile `json:"files"`
}

type BatchInfo struct {
	TotalPatients     int        `json:"totalPatients"`
	PatientsProcessed int        `json:"patientsProcessed"`
	PatientIndex      *int       `json:"patientIndex"`
	Status            string     `json:"status"`
	TransactionTime   time.Time  `json:"transactionTime"`
	SubmitTime        time.Time  `json:"submitTime"`
	CompleteTime      *time.Time `json:"completeTime"`
	RequestURL        string     `json:"requestURL"`
}

type BatchFile struct {
	ResourceType string `json:"resourceType"`
	BatchID      string `json:"batchID"`
	Sequence     int    `json:"sequence"`
	FileName     string `json:"fileName"`
	Count        int    `json:"count"`
	Checksum     string `json:"checksum"`
	FileLength   int    `json:"fileLength"`
}

func (f *BatchFile) FormOutputFileName() string {
	return fmt.Sprintf("%s-%d.%s", f.BatchID, f.Sequence, f.ResourceType)
}

type Output struct {
	Type      string                 `json:"type"`
	URL       string                 `json:"url"`
	Count     int                    `json:"count,omitempty"`
	Extension map[string]interface{} `json:"extension,omitempty"`
}

type Status struct {
	TransactionTime     time.Time              `json:"transactionTime"`
	Request             string                 `json:"request"`
	RequiresAccessToken bool                   `json:"requiresAccessToken"`
	Output              []Output               `json:"output"`
	Error               []Output               `json:"error"`
	Extension           map[string]interface{} `json:"extension"`
}
