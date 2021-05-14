package repository

import (
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker/v3"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type PatientRepositoryTest struct {
	suite.Suite
}

func TestPatientRepositoryTest(t *testing.T) {
	suite.Run(t, new(PatientRepositoryTest))
}

func (suite *PatientRepositoryTest) TestFindMBIsByGroupID() {
	db, mock := newMock()
	defer db.Close()
	repo := NewPatientRepo(db)
	groupID := faker.Word()
	expectedMBIs := []string{faker.UUIDDigit(), faker.UUIDDigit(), faker.UUIDDigit()}

	expectedInsertQuery := `SELECT patients.beneficiary_id FROM rosters JOIN attributions ON attributions.roster_id = rosters.id JOIN patients ON attributions.patient_id = patients.id WHERE rosters.id = \$1`

	rows := sqlmock.NewRows([]string{"beneficiary_id"}).
		AddRow(expectedMBIs[0]).
		AddRow(expectedMBIs[1]).
		AddRow(expectedMBIs[2])
	mock.ExpectQuery(expectedInsertQuery).WithArgs(groupID).WillReturnRows(rows)
	result, err := repo.FindMBIsByGroupID(groupID)
	if err := mock.ExpectationsWereMet(); err != nil {
		suite.T().Errorf("there were unfulfilled expectations: %s", err)
	}
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), expectedMBIs, result)
}

func (suite *JobRepositoryV1TestSuite) TestFindMBIsByGroupIDErrorInRepo() {
	db, mock := newMock()
	defer db.Close()
	repo := NewPatientRepo(db)
	groupID := faker.Word()

	expectedInsertQuery := `SELECT patients.beneficiary_id FROM rosters JOIN attributions ON attributions.roster_id = rosters.id JOIN patients ON attributions.patient_id = patients.id WHERE rosters.id = \$1`

	mock.ExpectQuery(expectedInsertQuery).WithArgs().WillReturnError(errors.New("Not enough arguments"))
	result, err := repo.FindMBIsByGroupID(groupID)
	if err2 := mock.ExpectationsWereMet(); err2 != nil {
		suite.T().Errorf("there were unfulfilled expectations: %s", err2)
	}
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), result)
}
