package repository

import (
	"context"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"testing"
)

type ImplementerOrgRepositoryTestSuite struct {
	suite.Suite
	fakeRel *model.ImplementerOrgRelation
}

func (suite *ImplementerOrgRepositoryTestSuite) SetupTest() {
	i := model.ImplementerOrgRelation{}
	_ = faker.FakeData(&i)
	suite.fakeRel = &i
	suite.fakeRel.Status = model.Unknown
}

func TestImplementerOrgRepositoryTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementerOrgRepositoryTestSuite))
}

func (suite *ImplementerOrgRepositoryTestSuite) TestFindRelation() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerOrgRepo(db)
	ctx := context.Background()
	expectedQuery := "SELECT id, implementer_id, organization_id, created_at, updated_at, deleted_at, status FROM implementer_org_relation WHERE implementer_id = \\$1 AND organization_id = \\$2"

	rows := sqlmock.NewRows([]string{"id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status"}).
		AddRow(suite.fakeRel.ID, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status)

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID).WillReturnRows(rows)

	rel, err := repo.FindRelation(ctx, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeRel.ID, rel.ID)
}

func (suite *ImplementerOrgRepositoryTestSuite) TestFindNonExistentRelation() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerOrgRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, implementer_id, organization_id, created_at, updated_at, deleted_at, status FROM implementer_org_relation WHERE implementer_id = \\$1 AND organization_id = \\$2"

	rows := sqlmock.NewRows([]string{"id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status"})
	//Did not add rows here, simulate missing record

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID).WillReturnRows(rows)

	Implementer, err := repo.FindRelation(ctx, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), Implementer)
}

func (suite *ImplementerOrgRepositoryTestSuite) TestInsert() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerOrgRepo(db)
	ctx := context.Background()

	expectedInsertQuery := "INSERT INTO implementer_org_relation \\(implementer_id, organization_id, status\\) VALUES \\(\\$1, \\$2, \\$3\\) returning id, implementer_id, organization_id, created_at, updated_at, deleted_at, status"

	rows := sqlmock.NewRows([]string{"id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status"}).
		AddRow(suite.fakeRel.ID, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.Status).WillReturnRows(rows)

	rel, err := repo.Insert(ctx, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.Status)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeRel.ID, rel.ID)
}
