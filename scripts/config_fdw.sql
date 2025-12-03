-- Run as master to setup the fdw from the dpc_queue DB to dpc_attribution.
-- This should be run in each env when the DB is configured for the first time.
-- The values you need can be found in the AWS Parameter Store.
/*
Run with:

psql -U {MASTER_DB_USER} \
-d dpc_queue \
-h {DB_ENDPOINT} \
-v ENV={ENV}
-v ATTRIBUTION_DB_USER={ATTRIBUTION_READ_ONLY_USER} \
-v ATTRIBUTION_DB_PASS='{ATTRIBUTION_READ_ONLY_DB_PASS}' \
-f scripts/config_fdw.sql
*/

CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE SERVER IF NOT EXISTS dpc_attribution
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (host 'localhost', dbname 'dpc_attribution', port '5432');

-- Create user mappings for any role we might be logged in as and give them read only access
CREATE OR REPLACE FUNCTION create_user_mapping(
    local_user TEXT,
    foreign_server TEXT,
    foreign_user TEXT,
    foreign_password TEXT
) RETURNS void AS $$
BEGIN
    EXECUTE format('CREATE USER MAPPING IF NOT EXISTS FOR %I SERVER %I OPTIONS (user ''%s'', password ''%s'')',
        local_user,
        foreign_server,
        foreign_user,
        foreign_password
    );
    EXECUTE format('GRANT USAGE ON FOREIGN DATA WRAPPER postgres_fdw TO %I',
        local_user
    );
END;
$$ LANGUAGE plpgsql;

SELECT create_user_mapping(:'ENV' || '-dpc_queue-role', 'dpc_attribution', :'ATTRIBUTION_DB_USER', :'ATTRIBUTION_DB_PASS');
SELECT create_user_mapping(:'ENV' || '-aggregation-dpc_queue-role', 'dpc_attribution', :'ATTRIBUTION_DB_USER', :'ATTRIBUTION_DB_PASS');
SELECT create_user_mapping(:'ENV' || '-aggregation-dpc_queue-read-only-role', 'dpc_attribution', :'ATTRIBUTION_DB_USER', :'ATTRIBUTION_DB_PASS');
SELECT create_user_mapping('postgres', 'dpc_attribution', :'ATTRIBUTION_DB_USER', :'ATTRIBUTION_DB_PASS');
