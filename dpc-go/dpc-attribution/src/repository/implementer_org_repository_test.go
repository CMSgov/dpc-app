package repository

import (
	"context"
	"fmt"
	"testing"

	"github.com/CMSgov/dpc/attribution/model"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker/v3"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ImplementerOrgRepositoryTestSuite struct {
	suite.Suite
	fakeRel *model.ImplementerOrgRelation
}

func (suite *ImplementerOrgRepositoryTestSuite) SetupTest() {
	i := model.ImplementerOrgRelation{}
	err := faker.FakeData(&i)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
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
	expectedQuery := "SELECT id, implementer_id, organization_id, created_at, updated_at, deleted_at, status, COALESCE\\(ssas_system_id, ''\\) FROM implementer_org_relations WHERE implementer_id = \\$1 AND organization_id = \\$2"

	rows := sqlmock.NewRows([]string{"id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status", "ssas_system_id"}).
		AddRow(suite.fakeRel.ID, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status, suite.fakeRel.SsasSystemID)

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID).WillReturnRows(rows)

	rel, err := repo.FindRelation(ctx, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeRel.ID, rel.ID)
}

func (suite *ImplementerOrgRepositoryTestSuite) TestFindManagedOrgs() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerOrgRepo(db)
	ctx := context.Background()
	expectedQuery := "SELECT id, implementer_id, organization_id, created_at, updated_at, deleted_at, status, COALESCE\\(ssas_system_id, ''\\) FROM implementer_org_relations WHERE implementer_id = \\$1 AND deleted_at IS NULL"

	rows := sqlmock.NewRows([]string{"id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status", "ssas_system_id"}).
		AddRow("00000000-0000-0000-0000-00000000000a", "00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002", suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status, suite.fakeRel.SsasSystemID).
		AddRow("00000000-0000-0000-0000-00000000000b", "00000000-0000-0000-0000-000000000003", "00000000-0000-0000-0000-000000000004", suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status, suite.fakeRel.SsasSystemID).
		AddRow("00000000-0000-0000-0000-00000000000c", "00000000-0000-0000-0000-000000000005", "00000000-0000-0000-0000-000000000006", suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status, suite.fakeRel.SsasSystemID)

	mock.ExpectQuery(expectedQuery).WithArgs(suite.fakeRel.ImplementerID).WillReturnRows(rows)

	orgs, err := repo.FindManagedOrgs(ctx, suite.fakeRel.ImplementerID)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), 3, len(orgs))
}

func (suite *ImplementerOrgRepositoryTestSuite) TestFindNonExistentRelation() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerOrgRepo(db)
	ctx := context.Background()

	expectedQuery := "SELECT id, implementer_id, organization_id, created_at, updated_at, deleted_at, status FROM implementer_org_relations WHERE implementer_id = \\$1 AND organization_id = \\$2"

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

	expectedInsertQuery := "INSERT INTO implementer_org_relations \\(implementer_id, organization_id, status\\) VALUES \\(\\$1, \\$2, \\$3\\) returning id, implementer_id, organization_id, created_at, updated_at, deleted_at, status, COALESCE\\(ssas_system_id, ''\\)"

	rows := sqlmock.NewRows([]string{"id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status", "ssas_system_id"}).
		AddRow(suite.fakeRel.ID, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status, suite.fakeRel.SsasSystemID)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.Status).WillReturnRows(rows)

	rel, err := repo.Insert(ctx, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.Status)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeRel.ID, rel.ID)
}

func (suite *ImplementerOrgRepositoryTestSuite) TestUpdate() {
	db, mock := newMock()
	defer db.Close()
	repo := NewImplementerOrgRepo(db)
	ctx := context.Background()
	sysId := faker.UUIDHyphenated()
	expectedInsertQuery := "UPDATE implementer_org_relations SET ssas_system_id = \\$1, updated_at = NOW\\(\\) WHERE implementer_id = \\$2 AND organization_id = \\$3 AND deleted_at IS NULL returning id, implementer_id, organization_id, created_at, updated_at, deleted_at, status, COALESCE\\(ssas_system_id, ''\\)"

	rows := sqlmock.NewRows([]string{"id", "implementer_id", "organization_id", "created_at", "updated_at", "deleted_at", "status", "ssas_system_id"}).
		AddRow(suite.fakeRel.ID, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, suite.fakeRel.CreatedAt, suite.fakeRel.UpdatedAt, nil, suite.fakeRel.Status, sysId)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(sysId, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID).WillReturnRows(rows)

	rel, err := repo.Update(ctx, suite.fakeRel.ImplementerID, suite.fakeRel.OrganizationID, sysId)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), sysId, rel.SsasSystemID)
}
