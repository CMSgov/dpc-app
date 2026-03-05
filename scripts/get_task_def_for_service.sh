#!/bin/bash

# Requires AWS CLI and session manager plugin.
# Returns the task definition of the currently running service

CLUSTER_NAME=$1
SERVICE_NAME=$2

TASK_ARN=$(aws ecs list-tasks --cluster "$CLUSTER_NAME" --service-name "$SERVICE_NAME" | jq .'taskArns')
TASK_DEFINITION_ARN=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" | jq -r '.tasks[].taskDefinitionArn')
TASK_DEFINITION=$(aws ecs describe-task-definition --task-definition "$TASK_DEFINITION_ARN")

echo "$TASK_DEFINITION" | jq
