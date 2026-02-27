#!/bin/bash

# Requires AWS CLI and session manager plugin.
# Gets the current task definition ARN for the service and checks if it's file system is writeable.  If it is, the ARN
# is returned.  If it isn't, a new writeable task definition is created in ECS and its ARN is returned.

CLUSTER_NAME=$1
SERVICE_NAME=$2

# Get the task def of the currently running service
TASK_DEFINITION=$(./get_task_def_for_service.sh "$CLUSTER_NAME" "$SERVICE_NAME")
READ_ONLY_ROOT_FILE_SYSTEM=$(echo "$TASK_DEFINITION" | jq '.taskDefinition.containerDefinitions[0].readonlyRootFilesystem' )

# Check if the task def already has a writeable file system.  If so, we can use it.
if [ "$READ_ONLY_ROOT_FILE_SYSTEM" == "false" ]; then
  echo "$TASK_DEFINITION" | jq -r '.taskDefinition.taskDefinitionArn'
  exit 0
fi

# Task wasn't writeable, so we need to register a new task def that is.
NEW_TASK_DEFINITION=$(echo "$TASK_DEFINITION" | jq '.taskDefinition')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq '.containerDefinitions[0].readonlyRootFilesystem = false')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq 'del(.taskDefinitionArn)')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq 'del(.revision)')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq 'del(.status)')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq 'del(.requiresAttributes)')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq 'del(.compatibilities)')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq 'del(.registeredAt)')
NEW_TASK_DEFINITION=$(echo "$NEW_TASK_DEFINITION" | jq 'del(.registeredBy)')

CREATED_TASK_DEFINITION=$(aws ecs register-task-definition --cli-input-json "$NEW_TASK_DEFINITION" )
echo "$CREATED_TASK_DEFINITION" | jq -r '.taskDefinition.taskDefinitionArn'
