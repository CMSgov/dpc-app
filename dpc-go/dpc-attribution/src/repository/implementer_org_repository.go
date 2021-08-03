package repository

import (
	"context"
	"database/sql"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/huandu/go-sqlbuilder"
)

// ImplementerOrgRepo is an interface for test mocking purposes
type ImplementerOrgRepo interface {
	Insert(ctx context.Context, implID string, orgID string, status model.ImplOrgStatus) (*model.ImplementerOrgRelation, error)
	FindRelation(ctx context.Context, implID string, orgID string) (*model.ImplementerOrgRelation, error)
	FindManagedOrgs(ctx context.Context, implID string) ([]model.ImplementerOrgRelation, error)
	Update(ctx context.Context, implID string, orgID string, sysID string) (*model.ImplementerOrgRelation, error)
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
func (or *ImplementerOrgRepository) FindRelation(ctx context.Context, implementerID string, orgID string) (*model.ImplementerOrgRelation, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status", "COALESCE(ssas_system_id, '')")
	sb.From("implementer_org_relations")
	sb.Where(sb.Equal("implementer_id", implementerID), sb.Equal("organization_id", orgID), sb.IsNull("deleted_at"))
	q, args := sb.Build()

	ior := new(model.ImplementerOrgRelation)
	iorStruct := sqlbuilder.NewStruct(new(model.ImplementerOrgRelation)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(iorStruct.Addr(&ior)...); err != nil {
		if err.Error() == "sql: no rows in result set" {
			return nil, nil
		}
		return nil, err
	}
	return ior, nil
}

// FindManagedOrgs function that searches the database for the orgs managed by an implementer
func (or *ImplementerOrgRepository) FindManagedOrgs(ctx context.Context, implementerID string) ([]model.ImplementerOrgRelation, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status", "COALESCE(ssas_system_id, '')")
	sb.From("implementer_org_relations")
	sb.Where(sb.Equal("implementer_id", implementerID), sb.IsNull("deleted_at"))
	q, args := sb.Build()

	rows, err := or.db.QueryContext(ctx, q, args...)
	if err != nil {
		return nil, err
	}

	defer rows.Close()
	result := make([]model.ImplementerOrgRelation, 0)
	for rows.Next() {
		ior := new(model.ImplementerOrgRelation)
		iorStruct := sqlbuilder.NewStruct(new(model.ImplementerOrgRelation)).For(sqlFlavor)
		err = rows.Scan(iorStruct.Addr(&ior)...)
		if err != nil {
			return nil, err
		}
		result = append(result, *ior)
	}
	return result, nil
}

// Insert function that saves the ImplementerOrgRelation model into the database and returns the v2.ImplementerOrgRelation
func (or *ImplementerOrgRepository) Insert(ctx context.Context, implID string, orgID string, status model.ImplOrgStatus) (*model.ImplementerOrgRelation, error) {
	implOrg := model.ImplementerOrgRelation{
		ImplementerID:  implID,
		OrganizationID: orgID,
		Status:         status,
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("implementer_org_relations")
	ib.Cols("implementer_id", "organization_id", "status")
	ib.Values(implOrg.ImplementerID, implOrg.OrganizationID, implOrg.Status)
	ib.SQL("returning id, implementer_id, organization_id, created_at, updated_at, deleted_at, status, COALESCE(ssas_system_id, '')")

	q, args := ib.Build()

	ior := new(model.ImplementerOrgRelation)
	iorStruct := sqlbuilder.NewStruct(new(model.ImplementerOrgRelation)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(iorStruct.Addr(&ior)...); err != nil {
		return nil, err
	}

	return ior, nil
}

// Update function that updates the ImplementerOrgRelation model and returns the v2.ImplementerOrgRelation
func (or *ImplementerOrgRepository) Update(ctx context.Context, implID string, orgID string, sysID string) (*model.ImplementerOrgRelation, error) {
	ub := sqlFlavor.NewUpdateBuilder()
	ub.Update("implementer_org_relations")
	ub.Set(
		ub.Assign("ssas_system_id", sysID),
		"updated_at = NOW()",
	)
	ub.Where(ub.Equal("implementer_id", implID), ub.Equal("organization_id", orgID), ub.IsNull("deleted_at"))
	ub.SQL("returning id, implementer_id, organization_id, created_at, updated_at, deleted_at, status, COALESCE(ssas_system_id, '')")
	q, args := ub.Build()

	relation := new(model.ImplementerOrgRelation)
	iorStruct := sqlbuilder.NewStruct(new(model.ImplementerOrgRelation)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(iorStruct.Addr(&relation)...); err != nil {
		return nil, err
	}

	return relation, nil
}
