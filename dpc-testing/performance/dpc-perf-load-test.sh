#!/bin/bash
docker pull loadimpact/k6

docker run -v $(pwd)/scripts:/scripts loadimpact/k6 run -e env=${ENV} /scripts/get_metadata-perf.js
docker run -v $(pwd)/scripts:/scripts loadimpact/k6 run -e env=${ENV} /scripts/get_metadata-load.js
