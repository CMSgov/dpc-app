#!/bin/bash

set -euxo pipefail

# Requires AWS CLI and session manager plugin.
# Starts a temporary copy of a service that supports shell access.  Remember to delete it manually when you're done
# with it!

# Run with: ~/scripts/start_temp_service.sh <CLUSTER> <SERVICE>

CLUSTER_NAME=$1
SERVICE_NAME=$2
INITIAL_TAG=$3

# Add your initials here
NEW_SERVICE_NAME="${INITIAL_TAG}_${SERVICE_NAME}"

# Check if the temp service is already up from a previous run, and if not start it.
NEW_SERVICE_COUNT=$(aws ecs describe-services --cluster "$CLUSTER_NAME" --services "$NEW_SERVICE_NAME" | jq -r '.services[].runningCount')
if [[ -z "$NEW_SERVICE_COUNT" || "$NEW_SERVICE_COUNT" -eq 0 ]]; then

  echo "Getting task definition."
  TASK_DEF_ARN=$(./dpc-app/scripts/get_writeable_task_def_for_service.sh "$CLUSTER_NAME" "$SERVICE_NAME")
  TASK_DEF=${TASK_DEF_ARN#*/}

  echo "Getting network config." # Gets security groups and subnets
  NETWORK_CONFIG=$(aws ecs describe-services --cluster "$CLUSTER_NAME" --services "$SERVICE_NAME" --query "services[0].networkConfiguration")

  echo "Starting new service $NEW_SERVICE_NAME"
  aws ecs create-service \
    --cluster "$CLUSTER_NAME" \
    --task-definition "$TASK_DEF" \
    --enable-execute-command \
    --service-name "$NEW_SERVICE_NAME" \
    --desired-count 1 \
    --launch-type FARGATE \
    --network-configuration "$NETWORK_CONFIG" > /dev/null

  echo "Waiting for $NEW_SERVICE_NAME to start."
  aws ecs wait services-stable --cluster "$CLUSTER_NAME" --services "$NEW_SERVICE_NAME"
else
  echo "$NEW_SERVICE_NAME is already running."
fi

echo "$NEW_SERVICE_NAME started, building login command."

echo "Getting running task for $NEW_SERVICE_NAME."
NEW_TASK_ARN=$(aws ecs list-tasks --cluster "$CLUSTER_NAME" --service-name "$NEW_SERVICE_NAME" --launch-type FARGATE | jq .'taskArns')

echo "Getting container info for $NEW_SERVICE_NAME."
# Filter out the aws-guardduty container from the list.
NEW_CONTAINER_INFO=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$NEW_TASK_ARN" --query "tasks[].containers[?!contains(name, 'aws-guardduty')]" | jq '.[][]')
NEW_CONTAINER_NAME=$(echo "$NEW_CONTAINER_INFO" | jq -r '.name')
NEW_TASK_ARN=$(echo "$NEW_CONTAINER_INFO" | jq -r '.taskArn')
NEW_TASK_ID=${NEW_TASK_ARN#*/*/}
echo "New task id: $NEW_TASK_ID"
echo "New container name: $NEW_CONTAINER_NAME"

# Set env vars for Github
if [ -n "$GITHUB_ACTIONS" ]
then
    {
        echo "TASK_DEF=$TASK_DEF";
        echo "NEW_TASK_ID=$NEW_TASK_ID";
        echo "NEW_SERVICE_NAME=$NEW_SERVICE_NAME"
        echo "NEW_CONTAINER_NAME=$NEW_CONTAINER_NAME"
    } >> "$GITHUB_ENV"
fi
