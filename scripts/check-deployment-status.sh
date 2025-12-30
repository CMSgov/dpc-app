#!/bin/bash

set -euxo pipefail

ENV="$1" # env
TARGET_GROUP="dpc-${ENV}-${2}" # target
SVC_NAME="dpc-${ENV}-${4}" # service
CLUSTER_NAME="dpc-${ENV}-${5}" # cluster
SVC_VERSION="$6" # service version

if [ $7 == 'true' ]; then
    echo "Skipping $SVC_NAME"
    exit 0
fi

# 1) Wait for ECS to certify deployment stability

echo "Waiting for ECS service ${SVC_NAME}-${SVC_VERSION} deployment to succeed..."

MAX_ATTEMPTS=90   # 90 attempts
SLEEP_SECONDS=10  # 10 seconds between attempts
TOTAL_TIMEOUT=$((MAX_ATTEMPTS * SLEEP_SECONDS)) # Total time: 900 seconds (15 minutes)
DEPLOYMENT_STATUS="TIMEOUT" # Initialize status flag for post-loop check

# Use a for loop to iterate a fixed number of times
for ((i=1; i<=MAX_ATTEMPTS; i++)); do

  echo "Attempt $i of $MAX_ATTEMPTS: Checking rollout state..."

  # 1. Get the current rollout state of the PRIMARY deployment
  set +e
  ROLLOUT_STATE=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "${SVC_NAME}-${SVC_VERSION}" \
    --query "services[0].deployments[?status == 'PRIMARY'].rolloutState" \
    --output text)
  set -e

  # --- Check Rollout State ---
  if [ "$ROLLOUT_STATE" == "COMPLETED" ]; then
    echo "Service deployment completed successfully."
    DEPLOYMENT_STATUS="SUCCESS"
    break

  elif [ "$ROLLOUT_STATE" == "FAILED" ]; then
    echo "Deployment failed (rolloutState is FAILED).This was detected by the deployment Circuit Breaker. Check ECS service events for more details."
    DEPLOYMENT_STATUS="FAILURE"
    break

  elif [[ "$ROLLOUT_STATE" == "None" ]] || [[ -z "$ROLLOUT_STATE" ]]; then
    echo "ERROR: AWS API call to ECS describe services failed."
    DEPLOYMENT_STATUS="FAILURE"
    break

  elif [ "$ROLLOUT_STATE" == "IN_PROGRESS" ]; then
    echo "Status: IN_PROGRESS. Continuing to wait..."
  fi

  # Wait before the next attempt, but only if we are not on the last attempt
  if [ $i -lt $MAX_ATTEMPTS ]; then
    sleep "${SLEEP_SECONDS}s"
  fi

done

# Check the deployment status flag set inside the loop.
if [ "$DEPLOYMENT_STATUS" == "FAILURE" ]; then
    exit 1 # Terminate the entire script if a terminal failure was detected.
fi

if [ "$DEPLOYMENT_STATUS" == "TIMEOUT" ]; then
    # If the status is still TIMEOUT, it means the loop completed 60 attempts without breaking.
    echo "TIMEOUT ERROR: Deployment did not stabilize or failed within ${TOTAL_TIMEOUT} seconds."
    exit 1
fi

echo "ECS service ${SVC_NAME}-${SVC_VERSION} stable"

# 2) Wait for ELB to verify target group health
echo "---"
echo "1. Checking for existing Target Group: $TARGET_GROUP"

set +e
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
  --names "$TARGET_GROUP" \
  --query "TargetGroups[0].{arn: TargetGroupArn}" \
  --output text)
set -e

if [ -z "$TARGET_GROUP_ARN" ]
then
  echo "ERROR: Target Group ${TARGET_GROUP} not found or naming mismatch. Skipping ELB health check and exiting as a failure."
  exit 1
else
  echo "Found Target Group ARN: $TARGET_GROUP_ARN"
fi

echo "Waiting for ELB target group ${TARGET_GROUP} to become stable..."

aws elbv2 wait target-in-service \
  --target-group-arn "${TARGET_GROUP_ARN}"

if [ $? -ne 0 ]; then
    echo "ELB target group health check failed or timed out."
    exit 1
fi

echo "Deployment successful and application layer confirmed healthy."
