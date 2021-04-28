package repository

import (
    "context"
    "database/sql"
    "encoding/json"
    "github.com/CMSgov/dpc/attribution/model"
    "github.com/huandu/go-sqlbuilder"
)

// ImplementorRepo is an interface for test mocking purposes
type ImplementorRepo interface {
	Insert(ctx context.Context, body []byte) (*model.Implementor, error)
}

// ImplementorRepository is a struct that defines what the repository has
type ImplementorRepository struct {
	db *sql.DB
}

// NewImplementorRepo function that creates a organizationRepository and returns it's reference
func NewImplementorRepo(db *sql.DB) *ImplementorRepository {
	return &ImplementorRepository{
		db,
	}
}

// Insert function that saves the implementor model into the database and returns the model.Implementor
func (or *ImplementorRepository) Insert(ctx context.Context, body []byte) (*model.Implementor, error) {
	var implementorModel model.Implementor
	if err := json.Unmarshal(body, &implementorModel); err != nil {
		return nil, err
	}

	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto(`"implementor"`)
	ib.Cols("name")
	ib.Values(implementorModel.Name)
	ib.SQL("returning id, name, created_at, updated_at, deleted_at")

	q, args := ib.Build()

	implementor := new(model.Implementor)
	implementorStruct := sqlbuilder.NewStruct(new(model.Implementor)).For(sqlFlavor)
	if err := or.db.QueryRowContext(ctx, q, args...).Scan(implementorStruct.Addr(&implementor)...); err != nil {
		return nil, err
	}

	return implementor, nil
}
