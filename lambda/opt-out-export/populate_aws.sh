#!/bin/bash

# Populate all of our ssm parameters
# If you run this more than once the puts will fail, but the next section will still find them and output the params to the screen.
aws --endpoint-url=http://localhost:4566 ssm put-parameter \
    --name "/dpc/local/attribution/db_read_only_user_dpc_attribution" \
    --value "postgres" \
    --type "String"

aws --endpoint-url=http://localhost:4566 ssm put-parameter \
    --name "/dpc/local/consent/db_read_only_user_dpc_consent" \
    --value "postgres" \
    --type "String"

aws --endpoint-url=http://localhost:4566 ssm put-parameter \
    --name "/dpc/local/attribution/db_read_only_pass_dpc_attribution" \
    --value "dpc-safe" \
    --type "String"

aws --endpoint-url=http://localhost:4566 ssm put-parameter \
    --name "/dpc/local/consent/db_read_only_pass_dpc_consent" \
    --value "dpc-safe" \
    --type "String"

# Output all of our parameters
aws --endpoint-url=http://localhost:4566 ssm get-parameter --name "/dpc/local/attribution/db_read_only_user_dpc_attribution"
aws --endpoint-url=http://localhost:4566 ssm get-parameter --name "/dpc/local/consent/db_read_only_user_dpc_consent"
aws --endpoint-url=http://localhost:4566 ssm get-parameter --name "/dpc/local/attribution/db_read_only_pass_dpc_attribution"
aws --endpoint-url=http://localhost:4566 ssm get-parameter --name "/dpc/local/consent/db_read_only_pass_dpc_consent"

# Create the S3 bucket we're going to upload to
aws --endpoint-url=http://localhost:4566 s3 mb s3://bfd-local-eft
aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket bfd-local-eft --acl public-read-write

# Output the bucket and permissions
aws --endpoint-url=http://localhost:4566 s3api list-buckets
aws --endpoint-url=http://localhost:4566 s3api get-bucket-acl --bucket bfd-local-eft
