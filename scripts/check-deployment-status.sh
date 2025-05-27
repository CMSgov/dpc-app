#!/bin/bash

set -euxo pipefail

ENV="$1" # env
TARGET_GROUP="dpc-${ENV}-${2}" # target
TASK_NAME="dpc-${ENV}-${3}" # task_name
SVC_NAME="dpc-${ENV}-${4}" # service
CLUSTER_NAME="dpc-${ENV}-${5}" # cluster
SVC_VERSION="$6" # service version

set +e
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
  --names "$TARGET_GROUP" \
  --query "TargetGroups[0].{arn: TargetGroupArn}" \
  --output text)
set -e

if [ -z "$TARGET_GROUP_ARN" ]
then
  echo "No target groups defined! Assuming deploying a new ECS cluster and exiting healthy..."
  exit 0
fi

# Verify the deployment's health
#
# Criteria:
# - Only "healthy" container instances are present in target group
# - "Running tasks" count matches "Desired tasks" count for the service
# - There are no task definitions marked "INACTIVE" currently in-service

# Stash the new task definition
set +e
ACTIVE_TASK=$(aws ecs list-task-definitions \
    --status ACTIVE \
    --query 'taskDefinitionArns[0]' \
    --family-prefix "$TASK_NAME" \
    --sort desc \
    --output text)
set -e

if [ -z "$ACTIVE_TASK" ]
then
  echo "No active task defined! Assuming deploying a new ECS cluster and exiting healthy..."
  exit 0
fi

# Try polling for 30 minutes max to verify a deployment's health
TIMEOUT=1800
ELAPSED=0
SLEEP_SECONDS=15
VIABLE=4
CHECKED=0

while true
do
  if [ "$ELAPSED" -ge "$TIMEOUT" ]
  then
    echo "Timeout waiting for healthy containers"
    exit 1
  fi

  sleep "${SLEEP_SECONDS}s"

  set +e
  RESULT=$(aws elbv2 describe-target-health \
    --target-group-arn "$TARGET_GROUP_ARN" \
    --query "TargetHealthDescriptions[*].{health: TargetHealth}" \
    --output text)
  set -e

  set +e
  HEALTH_CHECK=$(echo "$RESULT" | grep -E "unhealthy|initial|draining|unused")
  set -e

  RUNNING_COUNT=$(aws ecs describe-services --cluster "$CLUSTER_NAME" \
    --services "${SVC_NAME}-${SVC_VERSION}" \
    --query "services[?serviceName=='${SVC_NAME}-${SVC_VERSION}'].{r: runningCount}" \
    --output text)

  set +e
  RUNNING_TASKS=$(aws ecs describe-tasks \
    --tasks $(aws ecs list-tasks \
              --service-name "${SVC_NAME}-${SVC_VERSION}" \
              --cluster "$CLUSTER_NAME" \
              --query 'taskArns[*]' \
              --output text) \
    --cluster "$CLUSTER_NAME" \
    --query 'tasks[*].{def: taskDefinitionArn}' \
    --output text)
  set -e

  set +e
  ACTIVE_TASK_CHECK=$(echo "$RUNNING_TASKS" | grep -v "$ACTIVE_TASK")
  set -e

  set +e
  DESIRED_COUNT=$(aws ecs describe-services --cluster "$CLUSTER_NAME" \
    --services "${SVC_NAME}-${SVC_VERSION}" \
    --query "services[?serviceName=='${SVC_NAME}-${SVC_VERSION}'].{d: desiredCount}" \
    --output text)
  set -e

  if [ -z "$ACTIVE_TASK_CHECK" ] && [ -z "$HEALTH_CHECK" ] && [ "$RUNNING_COUNT" == "$DESIRED_COUNT" ]
  then
    if [ "$CHECKED" == "$VIABLE" ]
    then
      exit 0
    else
      CHECKED=$(($CHECKED + 1))
    fi
  fi

  ELAPSED=$(($ELAPSED + $SLEEP_SECONDS))
done
