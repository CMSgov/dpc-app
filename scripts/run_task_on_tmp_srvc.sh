#!/bin/bash

set -euxo pipefail

# Requires AWS CLI and session manager plugin.
# Starts a temporary copy of a service that runs the given command and deletes it on completion.

# Run with: ~/scripts/run_task_on_tmp_srvc.sh \
#             <REGION> \
#             <CLUSTER> \
#             <SERVICE> \
#             <COMMAND>

REGION=$1
CLUSTER_NAME=$2
SERVICE_NAME=$3
COMMAND=$4

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

echo "Getting task definition."
TASK_DEF_ARN=$("$SCRIPT_DIR/get_writeable_task_def_for_service.sh" "$CLUSTER_NAME" "$SERVICE_NAME")
TASK_DEF=${TASK_DEF_ARN#*/}

echo "Getting network config." # Gets security groups and subnets
NETWORK_CONFIG=$(aws ecs describe-services --cluster "$CLUSTER_NAME" --services "$SERVICE_NAME" --query "services[0].networkConfiguration")

echo "Getting container name"

# Get container name with either legacy naming convention (i.e. `dpc-test-async-portal`) or CDAP service module convention (i.e. `async-portal-v9`)
CONTAINER=$(aws ecs describe-task-definition \
  --task-definition "$TASK_DEF" \
  --query "taskDefinition.containerDefinitions[?contains('${SERVICE_NAME}', name)] | [0]" \
  --output json)

CONTAINER_NAME=$(jq -r '.name' <<< "$CONTAINER")

# Skip running the entry point script on our tmp task and just drop to a shell for our command
OVERRIDES=$(jq -n \
  --arg name "$CONTAINER_NAME" \
  --arg cmd "$COMMAND" \
  '{
    containerOverrides: [
      {
        name: $name,
        command: ["sh", "-c", $cmd]
      }
    ]
  }')

echo "Running command on temp task"
TASK_ARN=$(aws ecs run-task \
  --region "$REGION" \
  --cluster "$CLUSTER_NAME" \
  --task-definition "$TASK_DEF" \
  --launch-type FARGATE \
  --network-configuration "$NETWORK_CONFIG" \
  --overrides "$OVERRIDES" \
  --query 'tasks[0].taskArn' --output text)

aws ecs wait tasks-stopped --region "$REGION" --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN"
sleep 5
aws logs tail "$(jq -er '.logConfiguration.options."awslogs-group"' <<< "$CONTAINER")" \
  --region "$(jq -er '.logConfiguration.options."awslogs-region"' <<< "$CONTAINER")" \
  --log-stream-names "$(jq -er '.logConfiguration.options."awslogs-stream-prefix"' <<< "$CONTAINER")/$CONTAINER_NAME/${TASK_ARN##*/}" \
  --since 1h --format short | { grep -oE 'https?://[^[:space:]]+/accept' || [[ $? -eq 1 ]]; }
