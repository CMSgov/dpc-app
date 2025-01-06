#!/bin/bash

set -euo pipefail

usage()
{
cat << EOF
usage: $0 options

This script gets the current images that are deployed in an environment.

OPTIONS:
   -h      Show this message
   -e      DPC environment name (dev, test, prod-sbx, prod)
EOF
}

DPC_ENV_NAME=

while getopts "h:e:" OPTION
do
    case $OPTION in
        h)
            usage
            exit 1
            ;;
        e)
            DPC_ENV_NAME=$OPTARG
            ;;
    esac
done


if [[ "$DPC_ENV_NAME" != "dev" ]] && [[ "$DPC_ENV_NAME" != "test" ]] && [[ "$DPC_ENV_NAME" != "prod-sbx" ]] && [[ "$DPC_ENV_NAME" != "prod" ]]
then
    usage
    exit 1
fi

for cluster in frontend backend
do
    set +e
    services=$(aws ecs list-services --cluster dpc-"${DPC_ENV_NAME}"-"${cluster}" --query 'serviceArns' --output text)
    set -e
    for service in $services
    do
        set +e
        taskdef="$(aws ecs describe-services --cluster dpc-"${DPC_ENV_NAME}"-"${cluster}" --services "${service}" --query 'services[0].taskDefinition' --output text)"
        images+="$(aws ecs describe-task-definition --task-definition "${taskdef}" --query 'taskDefinition.containerDefinitions[0].image' --output text) "
        set -e
    done
done

unique_images=$(echo "${images}" | xargs -n1 | sort -u | uniq)

for image in $unique_images
do
    echo -e "${image}"
done
