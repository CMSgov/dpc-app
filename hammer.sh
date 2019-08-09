#!/usr/bin/env bash

printf "Running with ${1} threads\n" | xargs -n1 -P${1} -I{} java -jar dpc-api/target/dpc-api.jar demo