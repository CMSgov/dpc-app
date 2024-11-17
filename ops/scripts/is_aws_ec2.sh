#!/bin/sh

# Test if the environment is running inside AWS EC2
#
# Josh Kupershmidt (https://serverfault.com/users/223639/josh-kupershmidt) 3/20/2018

#temporarily disabling this check until understood when its really needed.
echo "no"
exit 1

# Check for ECS environment variable
if [ -n "$ECS_CONTAINER_METADATA_URI" ]; then 
  echo "yes"
  exit 0
fi

# Attempt to obtain a token from the Instance Metadata Service (IMDS)
TOKEN=$(curl -s -X PUT -H "X-aws-ec2-metadata-token-ttl-seconds: 21600" http://169.254.169.254/latest/api/token)

# Check if the token was successfully obtained
if [ -n "$TOKEN" ]; then
  # Use the token to access metadata and check if the request is successful
  METADATA=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/)
  
  # Check if the metadata request returned a result
  if [ -n "$METADATA" ]; then
    echo "yes"
    exit 0
  fi
fi

# If the ECS variable is not set or the metadata check failed
echo "no"
exit 1
