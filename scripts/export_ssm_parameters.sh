#!/bin/bash

# Array with values using "ENV_VAR_NAME:SSM_PARAMETER_NAME" format
PROJECT="dpc"
ENV="test"
APP="web-portal"
PREFIX="/${PROJECT}/${ENV}/${APP}"

ENV_SSM_MAPPINGS=(
    "CLIENT_ID:${PREFIX}/cpi_api_gw_client_id"
    "CLIENT_SECRET:${PREFIX}/cpi_api_gw_client_secret"
    "CPI_API_GW_TESTDATA:${PREFIX}/cpi_api_gw_testdata"
)

# 1. Validation: Check AWS Credentials
echo "Validating AWS credentials..."
if ! aws sts get-caller-identity --query "Arn" --output text > /dev/null 2>&1; then
    echo "Error: AWS credentials not found or expired."
    return 1 2>/dev/null || exit 1
fi

# 2. Collect all paths for the batch call by splitting the "ENV_VAR:SSM_PATH" entries
SSM_PATHS=""
for ENTRY in "${ENV_SSM_MAPPINGS[@]}"; do
    SSM_PATH="${ENTRY#*:}" # Extract everything after the colon
    SSM_PATHS="$SSM_PATHS $SSM_PATH"
done

echo "Fetching parameters in batch..."

# 3. Batch fetch (returns "Path Value" pairs)
# Use --output json to enable querying using jq in next step
JSON_RESPONSE=$(aws ssm get-parameters \
    --names $SSM_PATHS \
    --with-decryption \
    --output json)

# 4. Loop through our mappings and extract values using jq
for ENTRY in "${ENV_SSM_MAPPINGS[@]}"; do
    ENV_VAR="${ENTRY%%:*}"
    SSM_PATH="${ENTRY#*:}"

    echo "Searching for ENV_VAR: $ENV_VAR, SSM_PATH: $SSM_PATH"

    # Use jq to find the value where Name == SSM_PATH
    # The '-r' flag in jq is CRITICAL: it outputs "raw" text (unquoted)
    
    VALUE=$(echo "$JSON_RESPONSE" | jq -r --arg PATH "$SSM_PATH" \
        '.Parameters[] | select(.Name == $PATH) | .Value')

    if [ -n "$VALUE" ]; then
        export "$ENV_VAR"="$VALUE"
        # echo "Exported $ENV_VAR (Length: ${#VALUE} chars)"
    else
        echo "Error: $SSM_PATH not found."
        return 1 2>/dev/null || exit 1
    fi
done