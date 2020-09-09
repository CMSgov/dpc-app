# Performance and Load Tests

DPC performance and load tests use [k6](https://k6.io/).

## Running the tests
### Shell script
The performance and load tests may be run using the script `dpc-perf-load-test.sh`. This will run k6 with Docker. Specify the environment you wish to test by setting the environment variable `ENV`. The options are `docker`, `dev`, `test`, `sandbox`, and `prod`.
```
ENV=docker ./dpc-perf-load-test.sh
```

### With k6
After installing k6, you can run the tests directly. Specify the environment with the flag `-e env={environment}`, and the test script file. The environment options are `local`, `dev`, `test`, `sandbox`, and `prod`.
```
k6 run -e env=local scripts/get_metadata-perf.js
```
