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

	expectedIpAddresses := []string{"127.0.0.1/32", "127.0.0.2/32"}
	ipAddressResult := sqlmock.NewRows(ipAddressColumns).AddRow("127.0.0.1/32").AddRow("127.0.0.2/32")

	mock.ExpectQuery(ipAddressQuery).WillReturnRows(ipAddressResult)
	ipAddresses, err := getAuthData("user", "pass")
	assert.Equal(t, expectedIpAddresses, ipAddresses)
	assert.Equal(t, nil, err)

	createConnection = oricreateConnection
}

func mockCreateConnection(db *sql.DB) {
	createConnection = func(dbName string, dbUser string, dbPassword string) (*sql.DB, error) {
		return db, nil
	}
}
