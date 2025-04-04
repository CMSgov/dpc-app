# DPC Load Testing

A suite of load tests for the DPC API, running on K6 in GitHub Actions. 

Developer documentation for K6 can be found [on the official website](https://grafana.com/docs/k6/latest/). 

### Running the tests

#### In Github Actions

In GitHub Actions, the load tests must be manually triggered. Developers can run them [here](https://github.com/CMSgov/dpc-app/actions/workflows/dpc-load-test.yml).

#### Local Development

Environment variables are stored in Ansible Vault. These will all populate automatically when you run the make command. To test locally, follow these steps:
1. Run `yarn` in order to get IDE integration for K6 libraries. 
2. Run `make load-tests`.

This will trigger the build of a new API environment with `AUTH_DISABLED=true`. It then triggers the load test suite to run, and finally tears down the new API environment.

#### Unit Testing

A few of our utilities require unit tests, which live in the `__tests__` directory. Run unit tests by running `yarn test` from this directory.

### A note on modules

The K6 docker image that we rely on to run this both in GH Actions and locally does not actually run a Node.js environment. Therefore, we are unable to use most standard Node.js runtime utilities, including built-in library functions and the usual import syntax. 

A workaround for Node.js libraries is to pull them directly from a CDN, e.g. `import KJUR from 'https://unpkg.com/jsrsasign@11.1.0/lib/jsrsasign.js';`. Currently, we don't need any third-party packages; this note is for future reference.

Many utilities are provided by K6. See: https://jslib.k6.io/. In addition, K6 provides a [JavaScript API](https://grafana.com/docs/k6/latest/javascript-api/) with utilities including HTTP requests, various cryptographic functions, and more.

When importing local files, you'll need to include the file extension, e.g. `import generateDPCToken from './generate-dpc-token.js';`
