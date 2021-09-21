#!/usr/bin/env sh

set -ex

./migrate -path=./migrations/ -database "${DPC_DB_URL}?sslmode=disable" up

./bin/attribution 2>&1 | tee -a /var/log/dpc-attribution-go-$(hostname).log
 
