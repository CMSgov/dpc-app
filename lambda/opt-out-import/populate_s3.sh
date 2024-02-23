#!/bin/bash
aws --endpoint-url=http://localhost:4566 s3 mb s3://demo-bucket
aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket demo-bucket --acl public-read
aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket demo-bucket --acl public-write
aws --endpoint-url=http://localhost:4566 s3api put-object --bucket demo-bucket --key P#EFT.ON.DPC.NGD.RSP.D240123.T1122001 --body=synthetic_test_data/P#EFT.ON.DPC.NGD.RSP.D240123.T1122001
aws --endpoint-url=http://localhost:4566 s3api put-object --bucket demo-bucket --key dummyfile.txt --body=dummyfile.txt
