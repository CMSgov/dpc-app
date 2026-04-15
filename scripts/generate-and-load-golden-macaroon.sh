#!/usr/bin/env bash
set -euo pipefail

ENV="${1:?Usage: $0 <env>}"

echo "Retrieving ${ENV} ALB name"

ALB_URL=$(aws elbv2 describe-load-balancers \
  --names "dpc-${ENV}-frontend-internal" \
  | jq -r '.LoadBalancers[0].DNSName')

if [[ -z "${ALB_URL}" || "${ALB_URL}" == "null" ]]; then
  echo "ERROR: Could not retrieve ALB DNS name." >&2
  exit 1
fi

echo "Generating token"

TOKEN=""
for attempt in 1 2 3; do
  TOKEN=$(curl --fail --silent --show-error --max-time 30 \
    -X POST "http://${ALB_URL}:9900/tasks/generate-token" \
    | tr -d '[:space:]') && break
  echo "Attempt ${attempt} failed. Retrying in 5s..." >&2
  sleep 5
done

if [[ -z "${TOKEN}" ]]; then
  echo "ERROR: Failed to generate token after 3 attempts." >&2
  exit 1
fi

if ! echo "${TOKEN}" | base64 --decode > /dev/null 2>&1; then
  echo "ERROR: Token is not valid base64." >&2
  exit 1
fi

echo "Storing token in SSM"

aws ssm put-parameter \
  --name "/dpc/${ENV}/web/golden_macaroon" \
  --type "SecureString" \
  --value "${TOKEN}" \
  --key-id "alias/dpc-${ENV}-master-key" \
  --overwrite

echo "Golden Macaroon successfully stored in SSM."