# DPC Load Testing

A suite of load tests for the DPC API, running on K6 in GitHub Actions. 

Developer documentation for K6 can be found [on the official website](https://grafana.com/docs/k6/latest/). 

### Running the tests

In GitHub Actions, the load tests must be manually triggered. Developers can run them [here](https://github.com/CMSgov/dpc-app/actions/workflows/dpc-load-test.yml).

To test locally, run `make start-load-tests` from your CLI.

