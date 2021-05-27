package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"

	"github.com/CMSgov/dpc/attribution/model/v2"
	"github.com/huandu/go-sqlbuilder"
)

// ImplementerRepo is an interface for test mocking purposes
type ImplementerRepo interface {
	Insert(ctx context.Context, body []byte) (*v2.Implementer, error)
	FindByID(ctx context.Context, id string) (*v2.Implementer, error)
}

// ImplementerRepository is a struct that defines what the repository has
type ImplementerRepository struct {
	db *sql.DB
}

// NewImplementerRepo function that creates an ImplementerRepository and returns it's reference
func NewImplementerRepo(db *sql.DB) *ImplementerRepository {
	return &ImplementerRepository{
		db,
	}
}

// FindByID function that searches the database for the Implementer that matches the id
func (or *ImplementerRepository) FindByID(ctx context.Context, id string) (*v2.Implementer, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("id", "name", "created_at", "updated_at", "deleted_at")
	sb.From("implementers")
	sb.Where(sb.Equal("id", id))
	q, args := sb.Build()

	Implementer := new(v2.Implementer)
	ImplementerStruct := sqlbuilder.NewStruct(new(v2.Implementer)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(ImplementerStruct.Addr(&Implementer)...); err != nil {
		return nil, err
	}
	return Implementer, nil
}

// Insert function that saves the Implementer model into the database and returns the model.Implementer
func (or *ImplementerRepository) Insert(ctx context.Context, body []byte) (*v2.Implementer, error) {
	var ImplementerModel v2.Implementer
	if err := json.Unmarshal(body, &ImplementerModel); err != nil {
		return nil, err
	}

	if ImplementerModel.Name == "" {
		return nil, fmt.Errorf("missing required field: \"name\"")
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("implementers")
	ib.Cols("name")
	ib.Values(ImplementerModel.Name)
	ib.SQL("returning id, name, created_at, updated_at, deleted_at")

	q, args := ib.Build()

	Implementer := new(v2.Implementer)
	ImplementerStruct := sqlbuilder.NewStruct(new(v2.Implementer)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(ImplementerStruct.Addr(&Implementer)...); err != nil {
		return nil, err
	}

	return Implementer, nil
}
