#!/usr/bin/env sh

set -ex

./bin/api 2>&1 | tee -a /var/log/dpc-api-go-$(hostname).log
