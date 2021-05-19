package repository

import (
	"context"
	"database/sql"

	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/huandu/go-sqlbuilder"
)

// PatientRepo is an interface for test mocking purposes
type PatientRepo interface {
	FindMBIsByGroupID(groupID string) ([]string, error)
	GetGroupNPIs(ctx context.Context, groupID string) (*v1.GroupNPIs, error)
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
func (pr *PatientRepository) FindMBIsByGroupID(groupID string) ([]string, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("patients.beneficiary_id")
	sb.From("rosters")
	sb.Join("attributions", "attributions.roster_id = rosters.id")
	sb.Join("patients", "attributions.patient_id = patients.id")
	sb.Where(sb.Equal("rosters.id", groupID), sb.NotEqual("attributions.inactive", true))
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

// GetGroupNPIs function returns an organization NPI and a provider NPI for a given group ID
func (pr *PatientRepository) GetGroupNPIs(ctx context.Context, groupID string) (*v1.GroupNPIs, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("o.id_value, p.provider_id")
	sb.From("rosters r")
	sb.Join("organizations o", "r.organization_id = o.id")
	sb.Join("providers p", "r.provider_id = p.id")
	sb.Where(sb.Equal("r.id", groupID))
	q, args := sb.Build()

	row := new(v1.GroupNPIs)
	rowStruct := sqlbuilder.NewStruct(new(v1.GroupNPIs)).For(sqlFlavor)
	if err := pr.db.QueryRowContext(ctx, q, args...).Scan(rowStruct.Addr(&row)...); err != nil {
		return nil, err
	}
	return row, nil
}
