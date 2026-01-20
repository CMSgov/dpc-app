-- Run as master to setup alerts on view exports.  This sets up a trigger on insert or update to the
-- cron.job_run_details table and checks each job's status.  If it failed, a message is sent to Slack using the aws_lambda
-- extension.  If it's not already installed, you can install it with:
-- CREATE EXTENSION aws_lambda CASCADE;
/*
Run with:

psql -U {MASTER_DB_USER} \
-d postgres \
-h {DB_ENDPOINT} \
-f scripts/config_cron_alerts.sql
*/

CREATE OR REPLACE FUNCTION cron.check_cron_status() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'failed' THEN
        EXECUTE format($f$
            SELECT aws_lambda.invoke(
                aws_commons.create_lambda_function_arn('arn:aws:lambda:us-east-1:202533514245:function:bcda-prod-alarm-to-slack'),
                $rec$
                {
                  "Records": [
                    {
                      "messageId": "",
                      "body": "{\"Type\": \"Notification\", \"Message\": \"{\\\"AlarmName\\\": \\\"dpc-prod-Postgres Export Alert\\\", \\\"NewStateValue\\\": \\\"ALARM\\\", \\\"OldStateValue\\\": \\\"OK\\\", \\\"StateChangeTime\\\": \\\"%s\\\", \\\"AlarmDescription\\\": \\\"%s\\\"}\"}"
                    }
                  ]
                }
                $rec$::JSON
            );
        $f$,
            NOW(),
            CONCAT('runid=', NEW.runid, '.  ', REGEXP_REPLACE(NEW.return_message, '[\n\r"]', ' ', 'g'))
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Setup trigger
CREATE TRIGGER after_insert_update_job_run_details_trigger
    AFTER INSERT OR UPDATE ON cron.job_run_details
    FOR EACH ROW
EXECUTE FUNCTION cron.check_cron_status();
