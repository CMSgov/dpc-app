-- Run as master to setup the fdw from the dpc_queue DB to dpc_attribution.
-- This should be run in each env when the DB is configured for the first time.
-- The values you need can be found in the AWS Parameter Store.
/*
Run with:

psql -U {MASTER_DB_USER} \
-d dpc_queue \
-h {DB_ENDPOINT} \
-v QUEUE_DB_USER={QUEUE_DB_USER} \
-v ATTRIBUTION_DB_USER={ATTRIBUTION_READ_ONLY_USER} \
-v ATTRIBUTION_DB_PASS='{ATTRIBUTION_READ_ONLY_DB_PASS}' \
-f scripts/config_fdw.sql \
-a
*/

CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE SERVER IF NOT EXISTS dpc_attribution
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (host 'localhost', dbname 'dpc_attribution', port '5432');

CREATE USER MAPPING IF NOT EXISTS FOR :"QUEUE_DB_USER"
SERVER dpc_attribution
OPTIONS (user :'ATTRIBUTION_DB_USER', password :'ATTRIBUTION_DB_PASS');
