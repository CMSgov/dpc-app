package repository

import (
	"context"
	"database/sql"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/huandu/go-sqlbuilder"
)

// ImplementerOrgRepo is an interface for test mocking purposes
type ImplementerOrgRepo interface {
	Insert(ctx context.Context, implId string, orgId string, status model.ImplOrgStatus) (*model.ImplementerOrgRelation, error)
	FindRelation(ctx context.Context, implId string, orgId string) (*model.ImplementerOrgRelation, error)
}

// ImplementerOrgRepository is a struct that defines what the repository has
type ImplementerOrgRepository struct {
	db *sql.DB
}

// NewImplementerOrgRepo function that creates an ImplementerOrgRepository and returns it's reference
func NewImplementerOrgRepo(db *sql.DB) *ImplementerOrgRepository {
	return &ImplementerOrgRepository{
		db,
	}
}

// FindByID function that searches the database for the Implementer that matches the id
func (or *ImplementerOrgRepository) FindRelation(ctx context.Context, implementer_id string, org_id string) (*model.ImplementerOrgRelation, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status")
	sb.From("implementer_org_relation")
	sb.Where(sb.Equal("implementer_id", implementer_id), sb.Equal("organization_id", org_id))
	q, args := sb.Build()

	ior := new(model.ImplementerOrgRelation)
	iorStruct := sqlbuilder.NewStruct(new(model.ImplementerOrgRelation)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(iorStruct.Addr(&ior)...); err != nil {
		return nil, err
	}
	return ior, nil
}

// Insert function that saves the ImplementerOrgRelation model into the database and returns the model.ImplementerOrgRelation
func (or *ImplementerOrgRepository) Insert(ctx context.Context, implId string, orgId string, status model.ImplOrgStatus) (*model.ImplementerOrgRelation, error) {
	implOrg := model.ImplementerOrgRelation{
		ImplementerID:  implId,
		OrganizationID: orgId,
		Status:         status,
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("implementer_org_relation")
	ib.Cols("implementer_id", "organization_id", "status")
	ib.Values(implOrg.ImplementerID, implOrg.OrganizationID, implOrg.Status)
	ib.SQL("returning id, implementer_id, organization_id, created_at, updated_at, deleted_at, status")

	q, args := ib.Build()

	ior := new(model.ImplementerOrgRelation)
	iorStruct := sqlbuilder.NewStruct(new(model.ImplementerOrgRelation)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(iorStruct.Addr(&ior)...); err != nil {
		return nil, err
	}

	return ior, nil
}
