package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"

	"github.com/CMSgov/dpc/attribution/attributiontest"
	"github.com/CMSgov/dpc/attribution/model"

	"github.com/bxcodec/faker/v3"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

func newMock() (*sql.DB, sqlmock.Sqlmock) {
	db, mock, err := sqlmock.New()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	return db, mock
}

type OrganizationRepositoryTestSuite struct {
	suite.Suite
	fakeOrg *model.Organization
}

func (suite *OrganizationRepositoryTestSuite) SetupTest() {
	o := model.Organization{}
	err := faker.FakeData(&o)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	var i model.Info
	_ = json.Unmarshal([]byte(attributiontest.Orgjson), &i)
	o.Info = i
	suite.fakeOrg = &o
}

func TestOrganizationRepositoryTestSuite(t *testing.T) {
	suite.Run(t, new(OrganizationRepositoryTestSuite))
}

func (suite *OrganizationRepositoryTestSuite) TestFindByID() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, version, created_at, updated_at, info FROM organization WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"}).
		AddRow(suite.fakeOrg.ID, suite.fakeOrg.Version, suite.fakeOrg.CreatedAt, suite.fakeOrg.UpdatedAt, suite.fakeOrg.Info)

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeOrg.ID).WillReturnRows(rows)

	org, err := repo.FindByID(ctx, suite.fakeOrg.ID)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeOrg.ID, org.ID)
}

func (suite *OrganizationRepositoryTestSuite) TestFindByIDError() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, version, created_at, updated_at, info FROM organization WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"})

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeOrg.ID).WillReturnRows(rows)

	org, err := repo.FindByID(ctx, suite.fakeOrg.ID)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), org)
}

func (suite *OrganizationRepositoryTestSuite) TestInsertErrorExistingNPI() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()

	expectedCountQuery := "SELECT COUNT\\(\\*\\) as c FROM organization WHERE info @> '{\"identifier\": [{\"value\": \"?\"}]}"

	rows := sqlmock.NewRows([]string{"count"}).
		AddRow(1)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeOrg.Info)
	org, err := repo.Insert(ctx, b)
	assert.Nil(suite.T(), org)
	assert.Error(suite.T(), err)
}

func (suite *OrganizationRepositoryTestSuite) TestInsertErrorInRepo() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()

	expectedCountQuery := "SELECT COUNT\\(\\*\\) AS c FROM organization WHERE info @> '{\"identifier\": \\[{\"value\": \"\\d*\"}\\]}'"

	rows := sqlmock.NewRows([]string{"count"}).
		AddRow(0)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	expectedInsertQuery := "INSERT INTO organization \\(info\\) VALUES \\(\\$1\\) returning id, version, created_at, updated_at, info"

	rows = sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"})

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeOrg.Info).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeOrg.Info)
	org, err := repo.Insert(ctx, b)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), org)
}

func (suite *OrganizationRepositoryTestSuite) TestInsert() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()

	expectedCountQuery := "SELECT COUNT\\(\\*\\) AS c FROM organization WHERE info @> '{\"identifier\": \\[{\"value\": \"\\d*\"}\\]}'"

	rows := sqlmock.NewRows([]string{"count"}).
		AddRow(0)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	expectedInsertQuery := "INSERT INTO organization \\(info\\) VALUES \\(\\$1\\) returning id, version, created_at, updated_at, info"

	rows = sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"}).
		AddRow(suite.fakeOrg.ID, suite.fakeOrg.Version, suite.fakeOrg.CreatedAt, suite.fakeOrg.UpdatedAt, suite.fakeOrg.Info)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeOrg.Info).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeOrg.Info)
	org, err := repo.Insert(ctx, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeOrg.ID, org.ID)
}

func (suite *OrganizationRepositoryTestSuite) TestDelete() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()

	expectedQuery := "DELETE FROM organization WHERE id = \\$1"

	mock.ExpectExec(expectedQuery).WithArgs(suite.fakeOrg.ID).WillReturnError(errors.New("test"))

	err := repo.DeleteByID(ctx, suite.fakeOrg.ID)
	assert.Error(suite.T(), err)

	result := sqlmock.NewResult(1, 1)

	mock.ExpectExec(expectedQuery).WithArgs(suite.fakeOrg.ID).WillReturnResult(result)
	err = repo.DeleteByID(ctx, suite.fakeOrg.ID)
	assert.NoError(suite.T(), err)
}

func (suite *OrganizationRepositoryTestSuite) TestUpdate() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()
	b, _ := json.Marshal(suite.fakeOrg.Info)

	expectedCountQuery := "SELECT COUNT\\(\\*\\) AS c FROM organization WHERE info @> '{\"identifier\": \\[{\"value\": \"\\d*\"}\\]}' AND id <> \\$1"

	rows := sqlmock.NewRows([]string{"count"}).
		AddRow(0)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	expectedUpdatedQuery := "UPDATE organization SET version = version \\+ 1, info = \\$1, updated_at = now\\(\\) WHERE id = \\$2 returning id, version, created_at, updated_at, info"

	rows = sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"}).
		AddRow(suite.fakeOrg.ID, suite.fakeOrg.Version, suite.fakeOrg.CreatedAt, suite.fakeOrg.UpdatedAt, suite.fakeOrg.Info)

	mock.ExpectQuery(expectedUpdatedQuery).WithArgs(suite.fakeOrg.Info, suite.fakeOrg.ID).WillReturnRows(rows)

	org, err := repo.Update(ctx, suite.fakeOrg.ID, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeOrg.ID, org.ID)
}

func (suite *OrganizationRepositoryTestSuite) TestUpdateError() {
	db, mock := newMock()
	defer db.Close()
	repo := NewOrganizationRepo(db)
	ctx := context.Background()
	b, _ := json.Marshal(suite.fakeOrg.Info)

	expectedCountQuery := "SELECT COUNT\\(\\*\\) AS c FROM organization WHERE info @> '{\"identifier\": \\[{\"value\": \"\\d*\"}\\]}' AND id <> \\$1"
	expectedUpdatedQuery := "UPDATE organization SET version = version \\+ 1, info = \\$1, updated_at = now\\(\\) WHERE id = \\$2 returning id, version, created_at, updated_at, info"

	rows := sqlmock.NewRows([]string{"count"}).
		AddRow(1)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	org, err := repo.Update(ctx, suite.fakeOrg.ID, b)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), org)

	rows = sqlmock.NewRows([]string{"count"}).
		AddRow(0)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	mock.ExpectQuery(expectedUpdatedQuery).WithArgs(suite.fakeOrg.Info, suite.fakeOrg.ID).WillReturnError(errors.New("error"))

	org, err = repo.Update(ctx, suite.fakeOrg.ID, b)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), org)
}
