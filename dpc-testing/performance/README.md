# Performance and Load Tests

DPC performance and load tests are written in Go, using the [Vegeta](https://github.com/tsenart/vegeta) library.

## Running the tests

From this directory: `go run *.go --admin_url [admin URL] --api_url [API URL]` The admin URL is the base URL to the admin tasks endpoints. The API URL is the base URL to the user-facing API endpoints. For example, `go run *.go --admin_url http://localhost:9903/tasks --api_url http://localhost:3002/v1`
