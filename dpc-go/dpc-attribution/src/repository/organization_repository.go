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

// OrganizationRepo is an interface for test mocking purposes
type OrganizationRepo interface {
	Insert(ctx context.Context, body []byte) (*model.Organization, error)
	FindByID(ctx context.Context, id string) (*model.Organization, error)
	DeleteByID(ctx context.Context, id string) error
	Update(ctx context.Context, id string, body []byte) (*model.Organization, error)
	FindByNPI(ctx context.Context, npi string) (*model.Organization, error)
}

// OrganizationRepository is a struct that defines what the repository has
type OrganizationRepository struct {
	db *sql.DB
}

// NewOrganizationRepo function that creates a organizationRepository and returns it's reference
func NewOrganizationRepo(db *sql.DB) *OrganizationRepository {
	return &OrganizationRepository{
		db,
	}
}

// FindByID function that searches the database for the organization that matches the id
func (or *OrganizationRepository) FindByID(ctx context.Context, id string) (*model.Organization, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "version", "created_at", "updated_at", "info")
	sb.From("organization")
	sb.Where(sb.Equal("id", id))
	q, args := sb.Build()

	org := new(model.Organization)
	orgStruct := sqlbuilder.NewStruct(new(model.Organization)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}
	return org, nil
}

// Insert function that saves the fhir model into the database and returns the model.Organization
func (or *OrganizationRepository) Insert(ctx context.Context, body []byte) (*model.Organization, error) {

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
	sb.From("organization")
	sb.Where(fmt.Sprintf("info @> '{\"identifier\": [{\"value\": \"%s\"}]}'", npi))
	q, args := sb.Build()

	var count int
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(&count); err != nil {
		return nil, err
	}

	if count > 0 {
		return nil, errors.New("organization with npi already exists")
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("organization")
	ib.Cols("info")
	ib.Values(info)
	ib.SQL("returning id, version, created_at, updated_at, info")

	q, args = ib.Build()

	org := new(model.Organization)
	orgStruct := sqlbuilder.NewStruct(new(model.Organization)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}

	return org, nil
}

// DeleteByID function that deletes from the database the organization that matches the id
func (or *OrganizationRepository) DeleteByID(ctx context.Context, id string) error {
	db := sqlFlavor.NewDeleteBuilder()
	db.DeleteFrom("organization")
	db.Where(db.Equal("id", id))

	q, args := db.Build()

	_, err := or.db.ExecContext(ctx, q, args...)
	return err
}

// Update function that updates from the database the organization that matches the id
func (or *OrganizationRepository) Update(ctx context.Context, id string, body []byte) (*model.Organization, error) {

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
	sb.From("organization")
	sb.Where(fmt.Sprintf("info @> '{\"identifier\": [{\"value\": \"%s\"}]}'", npi), sb.NotEqual("id", id))

	q, args := sb.Build()

	var count int
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(&count); err != nil {
		return nil, err
	}

	if count > 0 {
		return nil, errors.New("organization with npi already exists")
	}

	ub := sqlFlavor.NewUpdateBuilder()
	ub.Update("organization").Set(
		ub.Incr("version"),
		ub.Assign("info", info),
		ub.Assign("updated_at", sqlbuilder.Raw("now()")),
	)
	ub.Where(ub.Equal("id", id))
	ub.SQL("returning id, version, created_at, updated_at, info")
	q, args = ub.Build()

	org := new(model.Organization)
	var orgStruct = sqlbuilder.NewStruct(new(model.Organization))
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}

	return org, nil
}

// FindByNPI function that searches the database for the organization that matches the id
func (or *OrganizationRepository) FindByNPI(ctx context.Context, npi string) (*model.Organization, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "version", "created_at", "updated_at", "info")
	sb.From("organization")
	sb.Where(fmt.Sprintf("info @> '{\"identifier\": [{\"value\": \"%s\"}]}'", npi))
	q, args := sb.Build()

	org := new(model.Organization)
	orgStruct := sqlbuilder.NewStruct(new(model.Organization)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}
	return org, nil
}
