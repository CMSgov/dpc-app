# alarm-to-slack
alarm to slack readme

## Purpose
This lambda receives SQS events and passes along the payloads to a slack webhook if its payload is a CloudWatch alarm sent via SNS.

## How to test the build locally and get a coverage report
Either run `make test` or `go test`. `make test` disables log output.