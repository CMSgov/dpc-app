package repository

import (
	"context"
	"database/sql"

	"github.com/CMSgov/dpc/attribution/model/v2"
	"github.com/huandu/go-sqlbuilder"
)

// ImplementerOrgRepo is an interface for test mocking purposes
type ImplementerOrgRepo interface {
	Insert(ctx context.Context, implID string, orgID string, status v2.ImplOrgStatus) (*v2.ImplementerOrgRelation, error)
	FindRelation(ctx context.Context, implID string, orgID string) (*v2.ImplementerOrgRelation, error)
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

// FindRelation function that searches the database for the relationship based in org and implementer id
func (or *ImplementerOrgRepository) FindRelation(ctx context.Context, implementerID string, orgID string) (*v2.ImplementerOrgRelation, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status")
	sb.From("implementer_org_relation")
	sb.Where(sb.Equal("implementer_id", implementerID), sb.Equal("organization_id", orgID))
	q, args := sb.Build()

	ior := new(v2.ImplementerOrgRelation)
	iorStruct := sqlbuilder.NewStruct(new(v2.ImplementerOrgRelation)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(iorStruct.Addr(&ior)...); err != nil {
		return nil, err
	}
	return ior, nil
}

// Insert function that saves the ImplementerOrgRelation model into the database and returns the model.ImplementerOrgRelation
func (or *ImplementerOrgRepository) Insert(ctx context.Context, implID string, orgID string, status v2.ImplOrgStatus) (*v2.ImplementerOrgRelation, error) {
	implOrg := v2.ImplementerOrgRelation{
		ImplementerID:  implID,
		OrganizationID: orgID,
		Status:         status,
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("implementer_org_relation")
	ib.Cols("implementer_id", "organization_id", "status")
	ib.Values(implOrg.ImplementerID, implOrg.OrganizationID, implOrg.Status)
	ib.SQL("returning id, implementer_id, organization_id, created_at, updated_at, deleted_at, status")

	q, args := ib.Build()

	ior := new(v2.ImplementerOrgRelation)
	iorStruct := sqlbuilder.NewStruct(new(v2.ImplementerOrgRelation)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(iorStruct.Addr(&ior)...); err != nil {
		return nil, err
	}

	return ior, nil
}
