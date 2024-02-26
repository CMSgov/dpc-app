package main

import "time"

const ImportInprog = "In-Progress"
const ImportComplete = "Completed"
const ImportFail = "Failed"

type OptOutFileEntity struct {
	id            string
	name          string
	timestamp     time.Time
	import_status string
	updated_at    string
}

type OptOutFilenameMetadata struct {
	Name         string
	Timestamp    time.Time
	FilePath     string
	Imported     bool
	DeliveryDate time.Time
	FileID       string
}

func (m OptOutFilenameMetadata) String() string {
	if m.FilePath != "" {
		return m.FilePath
	}
	return m.Name
}

type OptOutRecord struct {
	ID           string
	OptOutFileID string
	MBI          string
	PolicyCode   string
	EffectiveDt  time.Time
	Status       string
}

type ResponseFileRow struct {
	MBI               string `fixed:"1,11"`
	SharingPreference string `fixed:"12,12"`
}
