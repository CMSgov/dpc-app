package repository

import (
	"context"
	"database/sql"

	"github.com/huandu/go-sqlbuilder"

	"github.com/CMSgov/dpc/attribution/model"
	v2 "github.com/CMSgov/dpc/attribution/v2"
)

// GroupRepo is an interface for test mocking purposes
type GroupRepo interface {
	FindByID(ctx context.Context, id string) (*model.Group, error)
}

// GroupRepository is a struct that defines what the repository has
type GroupRepository struct {
	db *sql.DB
}

// NewGroupRepo function that creates a groupRepository and returns its reference
func NewGroupRepo(db *sql.DB) *GroupRepository {
	return &GroupRepository{
		db,
	}
}

// FindByID function that searches the database for the group that matches the group id and the token's organization id
func (or *GroupRepository) FindByID(ctx context.Context, id string) (*model.Group, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "provider_id", "organization_id")
	sb.From("rosters")
	sb.Where(sb.Equal("id", id)).And("organization_id", ctx.Value(v2.ContextKeyOrganization).(string))
	q, args := sb.Build()

	group := new(model.Group)
	groupStruct := sqlbuilder.NewStruct(new(model.Group)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(groupStruct.Addr(&group)...); err != nil {
		return nil, err
	}
	return group, nil
}
