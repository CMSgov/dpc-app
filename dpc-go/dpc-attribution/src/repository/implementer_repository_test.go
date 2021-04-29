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

type ImplementerRepositoryTestSuite struct {
	suite.Suite
	fakeImplementer *model.Implementer
}

func (suite *ImplementerRepositoryTestSuite) SetupTest() {
	i := model.Implementer{}
	_ = faker.FakeData(&i)
	suite.fakeImplementer = &i
}

func TestImplementerRepositoryTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementerRepositoryTestSuite))
}

func (suite *ImplementerRepositoryTestSuite) TestFindByID() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, name, created_at, updated_at, deleted_at FROM Implementer WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "name", "created_at", "updated_at", "deleted_at"}).
		AddRow(suite.fakeImplementer.ID, suite.fakeImplementer.Name, suite.fakeImplementer.CreatedAt, suite.fakeImplementer.UpdatedAt, nil)

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeImplementer.ID).WillReturnRows(rows)

	Implementer, err := repo.FindByID(ctx, suite.fakeImplementer.ID)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeImplementer.ID, Implementer.ID)
}

func (suite *ImplementerRepositoryTestSuite) TestFindByIDError() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, name, created_at, updated_at, deleted_at FROM Implementer WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "name", "created_at", "updated_at", "deleted_at"})

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeImplementer.ID).WillReturnRows(rows)

	Implementer, err := repo.FindByID(ctx, suite.fakeImplementer.ID)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), Implementer)
}

func (suite *ImplementerRepositoryTestSuite) TestInsert() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerRepo(db)
	ctx := context.Background()

	expectedInsertQuery := "INSERT INTO Implementer \\(name\\) VALUES \\(\\$1\\) returning id, name, created_at, updated_at, deleted_at"

	rows := sqlmock.NewRows([]string{"id", "name", "created_at", "updated_at", "deleted_at"}).
		AddRow(suite.fakeImplementer.ID, suite.fakeImplementer.Name, suite.fakeImplementer.CreatedAt, suite.fakeImplementer.UpdatedAt, nil)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeImplementer.Name).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeImplementer)
	Implementer, err := repo.Insert(ctx, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeImplementer.ID, Implementer.ID)
}
