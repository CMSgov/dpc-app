package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/attributiontest"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"log"
	"testing"
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
	_ = faker.FakeData(&o)
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
	repo := NewOrganizationRepo(db)
	defer repo.Close()
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
	repo := NewOrganizationRepo(db)
	defer repo.Close()
	ctx := context.Background()

	expectedQuery := "SELECT id, version, created_at, updated_at, info FROM organization WHERE id = \\$1"

	rows := sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"})

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeOrg.ID).WillReturnRows(rows)

	org, err := repo.FindByID(ctx, suite.fakeOrg.ID)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), org)
}

func (suite *OrganizationRepositoryTestSuite) TestCreateErrorExistingNPI() {
	db, mock := newMock()
	repo := NewOrganizationRepo(db)
	defer repo.Close()
	ctx := context.Background()

	expectedCountQuery := "SELECT COUNT\\(\\*\\) as c FROM organization WHERE info @> '{\"identifier\": [{\"value\": \"?\"}]}"

	rows := sqlmock.NewRows([]string{"count"}).
		AddRow(1)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeOrg.Info)
	org, err := repo.Create(ctx, b)
	assert.Nil(suite.T(), org)
	assert.Error(suite.T(), err)
}

func (suite *OrganizationRepositoryTestSuite) TestCreateErrorInRepo() {
	db, mock := newMock()
	repo := NewOrganizationRepo(db)
	defer repo.Close()
	ctx := context.Background()

	expectedCountQuery := "SELECT COUNT\\(\\*\\) AS c FROM organization WHERE info @> '{\"identifier\": \\[{\"value\": \"\\d*\"}\\]}'"

	rows := sqlmock.NewRows([]string{"count"}).
		AddRow(0)

	mock.ExpectQuery(expectedCountQuery).WillReturnRows(rows)

	expectedInsertQuery := "INSERT INTO organization \\(info\\) VALUES \\(\\$1\\) returning \\*"

	rows = sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"})

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeOrg.Info).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeOrg.Info)
	org, err := repo.Create(ctx, b)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), org)
}

func (suite *OrganizationRepositoryTestSuite) TestCreate() {
	db, mock := newMock()
	repo := NewOrganizationRepo(db)
	defer repo.Close()
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
	org, err := repo.Create(ctx, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeOrg.ID, org.ID)
}
