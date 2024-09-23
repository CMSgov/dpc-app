package main

import (
	"database/sql"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/stretchr/testify/assert"
)

func TestGetAuthData(t *testing.T) {
	ipAddressColumns := []string{"ip_address"}
	ipAddressQuery := "SELECT (.+) FROM ip_addresses"
	oricreateConnection := createConnection
	db, mock, err := sqlmock.New()
	mockCreateConnection(db)
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	tests := []struct {
		expectedIpAddresses []string
		err                 error
		ipAddressResult     *sqlmock.Rows
	}{
		{
			expectedIpAddresses: []string{"127.0.0.1"},
			err:                 nil,
			ipAddressResult:     sqlmock.NewRows(ipAddressColumns).AddRow("127.0.0.1"),
		},
	}

	for _, test := range tests {
		mock.ExpectQuery(ipAddressQuery).WillReturnRows(test.ipAddressResult)
		ipAddresses, err := getAuthData("user", "pass")
		assert.Equal(t, test.expectedIpAddresses, ipAddresses)
		assert.Equal(t, test.err, err)
	}
	createConnection = oricreateConnection
}

func mockCreateConnection(db *sql.DB) {
	createConnection = func(dbName string, dbUser string, dbPassword string) (*sql.DB, error) {
		return db, nil
	}
}
