package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/CMSgov/dpc/attribution/util"
	"github.com/huandu/go-sqlbuilder"
	"github.com/pkg/errors"
)

// PractitionerRepo is an interface for test mocking purposes
type PractitionerRepo interface {
	Insert(ctx context.Context, body []byte) (*model.Practitioner, error)
	FindByID(ctx context.Context, id string) (*model.Practitioner, error)
	DeleteByID(ctx context.Context, id string) error
	Update(ctx context.Context, id string, body []byte) (*model.Practitioner, error)
}

// PractitionerRepository is a struct that defines what the repository has
type PractitionerRepository struct {
	db *sql.DB
}

// NewPractitionerRepo function that creates a practitionerRepository and returns it's reference
func NewPractitionerRepo(db *sql.DB) *PractitionerRepository {
	return &PractitionerRepository{
		db,
	}
}

// FindByID function that searches the database for the practitioner that matches the id
func (or *PractitionerRepository) FindByID(ctx context.Context, id string) (*model.Practitioner, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "version", "created_at", "updated_at", "info")
	sb.From("practitioner")
	sb.Where(sb.Equal("id", id))
	q, args := sb.Build()

	org := new(model.Practitioner)
	orgStruct := sqlbuilder.NewStruct(new(model.Practitioner)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}
	return org, nil
}

// Insert function that saves the fhir model into the database and returns the model.Practitioner
func (or *PractitionerRepository) Insert(ctx context.Context, body []byte) (*model.Practitioner, error) {

	var info model.Info
	if err := json.Unmarshal(body, &info); err != nil {
		return nil, err
	}

	npi, err := util.GetNPI(body)
	if err != nil {
		return nil, err
	}

	sb := sqlFlavor.NewSelectBuilder()
	sb.Select(sb.As("COUNT(*)", "c"))
	sb.From("practitioner")
	sb.Where(fmt.Sprintf("info @> '{\"identifier\": [{\"value\": \"%s\"}]}'", npi))
	q, args := sb.Build()

	var count int
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(&count); err != nil {
		return nil, err
	}

	if count > 0 {
		return nil, errors.New("practitioner with npi already exists")
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("practitioner")
	ib.Cols("info")
	ib.Values(info)
	ib.SQL("returning id, version, created_at, updated_at, info")

	q, args = ib.Build()

	org := new(model.Practitioner)
	orgStruct := sqlbuilder.NewStruct(new(model.Practitioner)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}

	return org, nil
}

// DeleteByID function that deletes from the database the practitioner that matches the id
func (or *PractitionerRepository) DeleteByID(ctx context.Context, id string) error {
	db := sqlFlavor.NewDeleteBuilder()
	db.DeleteFrom("practitioner")
	db.Where(db.Equal("id", id))

	q, args := db.Build()

	_, err := or.db.ExecContext(ctx, q, args...)
	return err
}

// Update function that updates from the database the practitioner that matches the id
func (or *PractitionerRepository) Update(ctx context.Context, id string, body []byte) (*model.Practitioner, error) {

	var info model.Info
	if err := json.Unmarshal(body, &info); err != nil {
		return nil, err
	}

	npi, err := util.GetNPI(body)
	if err != nil {
		return nil, err
	}

	sb := sqlFlavor.NewSelectBuilder()
	sb.Select(sb.As("COUNT(*)", "c"))
	sb.From("practitioner")
	sb.Where(fmt.Sprintf("info @> '{\"identifier\": [{\"value\": \"%s\"}]}'", npi), sb.NotEqual("id", id))

	q, args := sb.Build()

	var count int
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(&count); err != nil {
		return nil, err
	}

	if count > 0 {
		return nil, errors.New("practitioner with npi already exists")
	}

	ub := sqlFlavor.NewUpdateBuilder()
	ub.Update("practitioner").Set(
		ub.Incr("version"),
		ub.Assign("info", info),
		ub.Assign("updated_at", sqlbuilder.Raw("now()")),
	)
	ub.Where(ub.Equal("id", id))
	ub.SQL("returning id, version, created_at, updated_at, info")
	q, args = ub.Build()

	org := new(model.Practitioner)
	var orgStruct = sqlbuilder.NewStruct(new(model.Practitioner))
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}

	return org, nil
}
