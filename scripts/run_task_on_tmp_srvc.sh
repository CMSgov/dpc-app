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

# We're relying on the fact that the container we want is named similar to the service.  For example, when the service is
# "dpc-dev-async-portal-v9", the container we want is "dpc-dev-async-portal".  If this is ever not the case we'll have to
# revisit, but for now this is the easiest way to filter out the datadog-agent, guard duty, service connect and whatever
# other containers we add to our services in the future.
echo "Getting container name"
CONTAINER_NAME=$(aws ecs describe-task-definition \
  --task-definition "$TASK_DEF" \
  --query "taskDefinition.containerDefinitions[?starts_with('${SERVICE_NAME}', name)].name | [0]" \
  --output text)

# Skip running the entry point script on our tmp task and just drop to a shell for our command
echo "Running command on temp task"
aws ecs run-task \
  --region "$REGION" \
  --cluster "$CLUSTER_NAME" \
  --task-definition "$TASK_DEF" \
  --launch-type FARGATE \
  --network-configuration "$NETWORK_CONFIG" \
  --overrides "{
    \"containerOverrides\": [
      {
        \"name\": \"$CONTAINER_NAME\",
        \"command\": [\"sh\", \"-c\", \"$COMMAND\"]
      }
    ]
  }" \
  --query 'tasks[0].taskArn' --output text
