package repository

import (
	"context"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/attributiontest"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"testing"
)

type GroupRepositoryTestSuite struct {
	suite.Suite
	fakeGrp *model.Group
}

func (suite *GroupRepositoryTestSuite) SetupTest() {
	g := model.Group{}
	_ = faker.FakeData(&g)
	var i model.Info
	_ = json.Unmarshal([]byte(attributiontest.Groupjson), &i)
	g.Info = i
	suite.fakeGrp = &g
}

func TestGroupRepositoryTestSuite(t *testing.T) {
	suite.Run(t, new(GroupRepositoryTestSuite))
}

func (suite *GroupRepositoryTestSuite) TestInsertErrorInRepo() {
	db, mock := newMock()
	defer db.Close()
	repo := NewGroupRepo(db)
	ctx := context.Background()

	expectedInsertQuery := `INSERT INTO group \(info\) VALUES \(\$1\) returning id, version, created_at, updated_at, info`

	rows := sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"})

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeGrp.Info).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeGrp.Info)
	org, err := repo.Insert(ctx, b)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), org)
}

func (suite *GroupRepositoryTestSuite) TestInsert() {
	db, mock := newMock()
	defer db.Close()
	repo := NewGroupRepo(db)
	ctx := context.Background()

	expectedInsertQuery := `INSERT INTO "group" \(info\) VALUES \(\$1\) returning id, version, created_at, updated_at, info`

	rows := sqlmock.NewRows([]string{"id", "version", "created_at", "updated_at", "info"}).
		AddRow(suite.fakeGrp.ID, suite.fakeGrp.Version, suite.fakeGrp.CreatedAt, suite.fakeGrp.UpdatedAt, suite.fakeGrp.Info)

	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeGrp.Info).WillReturnRows(rows)

	b, _ := json.Marshal(suite.fakeGrp.Info)
	org, err := repo.Insert(ctx, b)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeGrp.ID, org.ID)
}
