package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/huandu/go-sqlbuilder"
	"github.com/pkg/errors"
)

// GroupRepo is an interface for test mocking purposes
type GroupRepo interface {
	Insert(ctx context.Context, body []byte) (*model.Group, error)
}

// GroupRepository is a struct that defines what the repository has
type GroupRepository struct {
	db *sql.DB
}

// NewGroupRepo function that creates a organizationRepository and returns it's reference
func NewGroupRepo(db *sql.DB) *GroupRepository {
	return &GroupRepository{
		db,
	}
}

// Insert function that saves the fhir model into the database and returns the model.Group
func (or *GroupRepository) Insert(ctx context.Context, body []byte) (*model.Group, error) {
	log := logger.WithContext(ctx)
	organizationID, ok := ctx.Value(middleware.ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		return nil, errors.New("Failed to extract organization id from context")
	}

	var info model.Info
	if err := json.Unmarshal(body, &info); err != nil {
		return nil, err
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto(`"group"`)
	ib.Cols("info", "organization_id")
	ib.Values(info, organizationID)
	ib.SQL("returning id, version, created_at, updated_at, info, organization_id")

	q, args := ib.Build()

	group := new(model.Group)
	groupStruct := sqlbuilder.NewStruct(new(model.Group)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(groupStruct.Addr(&group)...); err != nil {
		return nil, err
	}

	return group, nil
}
