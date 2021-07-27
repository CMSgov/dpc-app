#!/bin/bash
client_secret=`docker-compose -p dpc-v2 -f docker-compose.yml -f docker-compose.v2.yml -f docker-compose.ssas-migrate.yml run --rm ssas --reset-secret --client-id=31e029ef-0e97-47f8-873c-0e8b7e7f99bf |tail -n1`
echo "SSAS Client ID: 31e029ef-0e97-47f8-873c-0e8b7e7f99bf"
echo "SSAS Client Secret: $client_secret"
