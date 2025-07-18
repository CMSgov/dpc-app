#!/bin/bash
aws --endpoint-url=http://localhost:4566 s3 mb s3://demo-bucket
aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket demo-bucket --acl public-read-write
aws --endpoint-url=http://localhost:4566 s3api put-object --bucket demo-bucket --key bfdeft01/dpc/in/T.NGD.DPC.RSP.D240123.T1122001.IN --body=synthetic_test_data/T.NGD.DPC.RSP.D240123.T1122001.IN
aws --endpoint-url=http://localhost:4566 s3api put-object --bucket demo-bucket --key bfdeft01/dpc/in/T.NGD.DPC.RSP.D010424.T1122001.IN --body=synthetic_test_data/T.NGD.DPC.RSP.D010424.T1122001.IN
aws --endpoint-url=http://localhost:4566 s3api put-object --bucket demo-bucket --key dummyfile.txt --body=dummyfile.txt
