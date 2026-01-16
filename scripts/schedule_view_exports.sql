-- Run as master to setup view exports to S3.  Requires that the aws_s3 extension is installed on each DB
-- that views will be exported from.  You can install it by going to the DB and running:
-- CREATE EXTENSION aws_s3 CASCADE;
/*
Run with:

psql -U {MASTER_DB_USER} \
-d postgres \
-h {DB_ENDPOINT} \
-f scripts/schedule_view_exports.sql
*/

-- Create a job in the cron.job table.  Jobs will be overwritten if they have the same job name, so this is idempotent.
CREATE OR REPLACE FUNCTION create_job(
    job_name TEXT,
    cron TEXT,
    db_name TEXT
) RETURNS void AS $$
BEGIN
    EXECUTE format($f$
            SELECT cron.schedule_in_database(
                '%s',
                '%s',
                $job$ SELECT * from aws_s3.query_export_to_s3('select * from %s',
                  aws_commons.create_s3_uri(
                    'dpc-prod-aurora-export-20251028154132844600000001',
                    CONCAT('%s_', TO_CHAR(NOW(), 'YYYY-MM-DD_HH24-MI-SS'), '.csv'),
                    'us-east-1'
                  )
                ) $job$,
                '%s'
            );
        $f$,
        job_name,
        cron,
        job_name,
        job_name,
        db_name
    );
END;
$$ LANGUAGE plpgsql;

SELECT create_job('benes_requested_last_week', '0 */6 * * *', 'dpc_queue');
SELECT create_job('bulk_calls_made_last_week', '5 */6 * * *', 'dpc_queue');
SELECT create_job('patient_everything_requests_last_week', '10 */6 * * *', 'dpc_queue');
SELECT create_job('since_requests_last_week', '15 */6 * * *', 'dpc_queue');
SELECT create_job('unique_benes_served_last_week', '20 */6 * * *', 'dpc_queue');
SELECT create_job('unique_providers_served_all_time', '25 */6 * * *', 'dpc_queue');
SELECT create_job('hcos_credentialed_all_time', '30 */6 * * *', 'dpc_auth');
SELECT create_job('hcos_with_active_credentials', '35 */6 * * *', 'dpc_auth');
