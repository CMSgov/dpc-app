# Smoke Tests

Smoke tests are provided by [Taurus](https://github.com/Blazemeter/taurus) and [JMeter](https://jmeter.apache.org).

## Installation

In order to run the tests, you'll need to ensure that `virtualenv` is installed.

```bash
pip3 install virtualenv
```

## Running Tests

The tests can be run by the environment-specific Makefile commands (e.g., `make smoke/local` will run the smoke tests against the locally running Docker instances.)
