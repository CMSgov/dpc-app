#!/bin/bash

RUNNING=`docker ps -q`
if [ -n "$RUNNING" ]; then
    docker stop $(docker ps -q)
fi
#docker system prune -a --volumes -f
docker rmi $(docker images --filter "dangling=true" -q --no-trunc)
