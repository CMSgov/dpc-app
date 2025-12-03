#!/bin/bash

set -euxo pipefail

ENV="$1" # env
TARGET_GROUP="dpc-${ENV}-${2}" # target
SVC_NAME="dpc-${ENV}-${4}" # service
CLUSTER_NAME="dpc-${ENV}-${5}" # cluster
SVC_VERSION="$6" # service version

# 1) Wait for ECS to certify deployment stability

echo "Waiting for ECS service ${SVC_NAME}-${SVC_VERSION} to become stable..."

aws ecs wait services-stable \
  --cluster "$CLUSTER_NAME" \
  --services "${SVC_NAME}-${SVC_VERSION}"

if [ $? -ne 0 ]; then
    echo "ECS deployment failed or timed out. Circuit Breaker may have initiated a rollback."
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
  --target-group-arn ${TARGET_GROUP_ARN}

if [ $? -ne 0 ]; then
    echo "ELB target group health check failed or timed out."
    exit 1
fi

echo "Deployment successful and application layer confirmed healthy."
