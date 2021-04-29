package repository

import (
	"context"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"testing"
)

type ImplementorRepositoryTestSuite struct {
	suite.Suite
	fakeImplementor *model.Implementor
}

func (suite *ImplementorRepositoryTestSuite) SetupTest() {
	i := model.Implementor{}
	_ = faker.FakeData(&i)
	suite.fakeImplementor = &i
}

func TestImplementorRepositoryTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementorRepositoryTestSuite))
}

func (suite *ImplementorRepositoryTestSuite) TestFindByID() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementorRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, name, created_at, updated_at, deleted_at FROM implementor WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "name", "created_at", "updated_at", "deleted_at"}).
		AddRow(suite.fakeImplementor.ID, suite.fakeImplementor.Name, suite.fakeImplementor.CreatedAt, suite.fakeImplementor.UpdatedAt, nil)

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeImplementor.ID).WillReturnRows(rows)

	implementor, err := repo.FindByID(ctx, suite.fakeImplementor.ID)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeImplementor.ID, implementor.ID)
}

func (suite *ImplementorRepositoryTestSuite) TestFindByIDError() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementorRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, name, created_at, updated_at, deleted_at FROM implementor WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "name", "created_at", "updated_at", "deleted_at"})

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeImplementor.ID).WillReturnRows(rows)

	implementor, err := repo.FindByID(ctx, suite.fakeImplementor.ID)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), implementor)
}

func (suite *ImplementorRepositoryTestSuite) TestInsert() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementorRepo(db)
	ctx := context.Background()

	expectedInsertQuery := "INSERT INTO implementor \\(name\\) VALUES \\(\\$1\\) returning id, name, created_at, updated_at, deleted_at"

	rows := sqlmock.NewRows([]string{"id", "name", "created_at", "updated_at", "deleted_at"}).
		AddRow(suite.fakeImplementor.ID, suite.fakeImplementor.Name, suite.fakeImplementor.CreatedAt, suite.fakeImplementor.UpdatedAt, nil)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeImplementor.Name).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeImplementor)
	implementor, err := repo.Insert(ctx, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeImplementor.ID, implementor.ID)
}
