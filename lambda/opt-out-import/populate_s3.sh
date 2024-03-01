#!/bin/bash
aws --endpoint-url=http://localhost:4566 s3 mb s3://demo-bucket
aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket demo-bucket --acl public-read
aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket demo-bucket --acl public-write
aws --endpoint-url=http://localhost:4566 s3api put-object --bucket demo-bucket --key T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009 --body=synthetic_test_data/T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009
aws --endpoint-url=http://localhost:4566 s3api put-object --bucket demo-bucket --key dummyfile.txt --body=dummyfile.txt

