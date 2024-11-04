#!/bin/sh

# Test if the environment is running inside AWS EC2
#
# Josh Kupershmidt (https://serverfault.com/users/223639/josh-kupershmidt) 3/20/2018

if [ -n "$ECS_CONTAINER_METADATA_URI" ]; then 
  echo "yes"
  exit 0
else
  echo "no"
  exit 1
fi
