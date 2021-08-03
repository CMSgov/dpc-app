package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"testing"

	"github.com/CMSgov/dpc/attribution/model"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker/v3"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ImplementerRepositoryTestSuite struct {
	suite.Suite
	fakeImplementer *model.Implementer
}

func (suite *ImplementerRepositoryTestSuite) SetupTest() {
	i := model.Implementer{}
	err := faker.FakeData(&i)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
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

	expectedQuery := "SELECT id, name, COALESCE\\(ssas_group_id, ''\\), created_at, updated_at, deleted_at FROM implementers WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "name", "ssas_group_id", "created_at", "updated_at", "deleted_at"}).
		AddRow(suite.fakeImplementer.ID, suite.fakeImplementer.SsasGroupID, suite.fakeImplementer.Name, suite.fakeImplementer.CreatedAt, suite.fakeImplementer.UpdatedAt, nil)

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

	expectedInsertQuery := "INSERT INTO implementers \\(name, ssas_group_id\\) VALUES \\(\\$1, \\$2\\) returning id, name, COALESCE\\(ssas_group_id, ''\\), created_at, updated_at, deleted_at"

	rows := sqlmock.NewRows([]string{"id", "name", "ssas_group_id", "created_at", "updated_at", "deleted_at"}).
		AddRow(suite.fakeImplementer.ID, suite.fakeImplementer.Name, suite.fakeImplementer.SsasGroupID, suite.fakeImplementer.CreatedAt, suite.fakeImplementer.UpdatedAt, nil)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeImplementer.Name, suite.fakeImplementer.SsasGroupID).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeImplementer)
	impl, err := repo.Insert(ctx, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeImplementer.ID, impl.ID)
}

func (suite *ImplementerRepositoryTestSuite) TestUpdate() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerRepo(db)
	ctx := context.Background()

	expectedInsertQuery := "UPDATE implementers SET name = \\$1, ssas_group_id = \\$2, updated_at = NOW\\(\\) WHERE id = \\$3 AND deleted_at IS NULL returning id, name, COALESCE\\(ssas_group_id, ''\\), created_at, updated_at, deleted_at"

	rows := sqlmock.NewRows([]string{"id", "name", "ssas_group_id", "created_at", "updated_at", "deleted_at"}).
		AddRow(suite.fakeImplementer.ID, suite.fakeImplementer.Name, suite.fakeImplementer.SsasGroupID, suite.fakeImplementer.CreatedAt, suite.fakeImplementer.UpdatedAt, nil)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeImplementer.Name, suite.fakeImplementer.SsasGroupID, suite.fakeImplementer.ID).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeImplementer)
	impl, err := repo.Update(ctx, suite.fakeImplementer.ID, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeImplementer.ID, impl.ID)
}
