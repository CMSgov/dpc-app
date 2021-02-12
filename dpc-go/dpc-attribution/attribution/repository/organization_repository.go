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

const sqlFlavor = sqlbuilder.PostgreSQL

type OrganizationRepo interface {
	Create(ctx context.Context, body []byte) (*model.Organization, error)
	FindByID(ctx context.Context, id string) (*model.Organization, error)
}

type OrganizationRepository struct {
	db *sql.DB
}

func NewOrganizationRepo(db *sql.DB) *OrganizationRepository {
	return &OrganizationRepository{
		db,
	}
}

func (or *OrganizationRepository) Close() {
	or.db.Close()
}

func (or *OrganizationRepository) FindByID(ctx context.Context, id string) (*model.Organization, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("*")
	sb.From("organization")
	sb.Where(sb.Equal("id", id))
	q, args := sb.Build()

	org := new(model.Organization)
	var orgStruct = sqlbuilder.NewStruct(new(model.Organization))
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}
	return org, nil
}

func (or *OrganizationRepository) Create(ctx context.Context, body []byte) (*model.Organization, error) {

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
	ib.SQL("returning *")

	q, args = ib.Build()

	org := new(model.Organization)
	var orgStruct = sqlbuilder.NewStruct(new(model.Organization))
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(orgStruct.Addr(&org)...); err != nil {
		return nil, err
	}

	return org, nil
}
