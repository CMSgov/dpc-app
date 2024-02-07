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
	ID                       string
	OptOutFileID             string
	MBI                      string `fixed:"1,11"`
	BeneficiaryLinkKeyString string `fixed:"12,21"`
	BeneficiaryLinkKey       int
	BeneficiaryFName         string `fixed:"22,51"`
	BeneficiaryMName         string `fixed:"52,81"`
	BeneficiaryLName         string `fixed:"82,121"`
	BeneficiaryDoB           string `fixed:"122,129"`
	BeneficiaryAddr1         string `fixed:"130,184"`
	BeneficiaryAddr2         string `fixed:"185,239"`
	BeneficiaryAddr3         string `fixed:"240,294"`
	BeneficiaryCity          string `fixed:"295,334"`
	BeneficiaryState         string `fixed:"335,336"`
	BeneficiaryZip5          string `fixed:"337,341"`
	BeneficiaryZip4          string `fixed:"342,345"`
	BeneficiaryGender        string `fixed:"346,346"`
	EncounterDt              string `fixed:"347,354"`
	EffectiveDtString        string `fixed:"355,362"`
	EffectiveDt              time.Time
	SourceCode               string `fixed:"363,367"`
	DataSharingCode          string `fixed:"368,368"`
	PrefIndicator            string `fixed:"369,369"`
	SAMHSAEffectiveDtString  string `fixed:"370,377"`
	SAMHSAEffectiveDt        time.Time
	SAMHSASourceCode         string `fixed:"378,382"`
	SAMHSAMechanismCode      string `fixed:"383,383"`
	SAMHSAPrefIndicator      string `fixed:"384,384"`
	ACOCMSID                 string `fixed:"385,394"`
	ACOLegalName             string `fixed:"395,464"`
	Status                   string `fixed:"465,474"`
	Reason                   string `fixed:"475,476"`
}
