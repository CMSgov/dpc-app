# DPC Load Testing

A suite of load tests for the DPC API, running on K6 in GitHub Actions. 

Developer documentation for K6 can be found [on the official website](https://grafana.com/docs/k6/latest/). 

### A note on modules

The K6 docker image that we rely on to run this both in GH Actions and locally does not actually run a Node.js environment. Therefore, we are unable to use any standard Node.js runtime utilities, including built-in library functions, normal imports, and NPM packages. 

Many utilities are provided by K6. See: https://jslib.k6.io/. In addition, K6 provides a [JavaScript API](https://grafana.com/docs/k6/latest/javascript-api/) with utilities including HTTP requests, various cryptographic functions, and more.

When importing local files, you'll need to include the file extension, e.g. `import generateDPCToken from './generate-dpc-token.js';`

### Running the tests

In GitHub Actions, the load tests must be manually triggered. Developers can run them [here](https://github.com/CMSgov/dpc-app/actions/workflows/dpc-load-test.yml).

To test locally, run `make start-load-tests` from your CLI.
