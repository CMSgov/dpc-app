package repository

import (
	"database/sql"
)

// PatientRepo is an interface for test mocking purposes
type PatientRepo interface {
	FindMBIsByGroupID(groupId string) ([]string, error)
}

// PatientRepository is a struct that defines what the repository has
type PatientRepository struct {
	db *sql.DB
}

// NewPatientRepo function that creates a patientRepository and returns it's reference
func NewPatientRepo(db *sql.DB) *PatientRepository {
	return &PatientRepository{
		db,
	}
}

// FindMBIsByGroupID function that searches the v1 attribution database for the patient MBIs for a given Group id
func (pr *PatientRepository) FindMBIsByGroupID(groupId string) ([]string, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("patients.beneficiary_id")
	sb.From("rosters")
	sb.Join("patients", "rosters.id = patients.roster_id")
	sb.Where(sb.Equal("rosters.id", groupId))
	q, args := sb.Build()

	var (
		MBIs       []string
		patientMBI string
	)

	rows, err := pr.db.Query(q, args...)
	if err != nil {
		return nil, err
	}
	for rows.Next() {
		if err := rows.Scan(&patientMBI); err != nil {
			return nil, err
		}
		MBIs = append(MBIs, patientMBI)
	}
	return MBIs, nil
}
