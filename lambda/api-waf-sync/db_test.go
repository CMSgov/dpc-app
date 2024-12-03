package main

import (
	"database/sql"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/stretchr/testify/assert"
)

func TestGetAuthData(t *testing.T) {
	ipAddressColumns := []string{"ip_address"}
	ipAddressQuery := "SELECT ip_address FROM ip_addresses"
	oricreateConnection := createConnection
	db, mock, err := sqlmock.New()
	mockCreateConnection(db)
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	expectedIpAddresses := []string{"127.0.0.1/32", "127.0.0.2/32"}
	ipAddressResult := sqlmock.NewRows(ipAddressColumns).AddRow("127.0.0.1").AddRow("127.0.0.2")

	mock.ExpectQuery(ipAddressQuery).WillReturnRows(ipAddressResult)
	ipAddresses, err := getAuthData("user", "pass")
	assert.Equal(t, expectedIpAddresses, ipAddresses)
	assert.Nil(t, err)

	createConnection = oricreateConnection
}

func mockCreateConnection(db *sql.DB) {
	createConnection = func(dbName string, dbUser string, dbPassword string) (*sql.DB, error) {
		return db, nil
	}
}
