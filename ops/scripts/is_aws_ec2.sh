#!/bin/sh

# Test if the environment is running inside AWS EC2
#
# Josh Kupershmidt (https://serverfault.com/users/223639/josh-kupershmidt) 3/20/2018

echo "I am testing if this is AWS EC2...";
# This first, simple check will work for many older instance types.
if [ -f /sys/hypervisor/uuid ]; then
  echo "I found the uuid file for the hypervisor!"
  # File should be readable by non-root users.
  if [ "$( head -c 3 /sys/hypervisor/uuid )" = "ec2" ]; then
    echo "Yes its an ec2 uuid!"
    echo "yes"
    exit 0
  else
    echo "Nope its not an ec2 uuid: " $( cat /sys/hypervisor/uuid );
    echo "no"
    #exit 1
  fi
fi

# This check will work on newer m5/c5 instances, but only if you have root!
if [ -r /sys/devices/virtual/dmi/id/product_uuid ]; then
  echo "Found the product uuid for the dmi!";
  # If the file exists AND is readable by us, we can rely on it.
  if [ "$( head -c 3 /sys/devices/virtual/dmi/id/product_uuid )" = "EC2" ]; then
    echo "Yes its an ec2 uuid!"
    echo "yes" 
    exit 0
  else
    echo "Nope! Not ec2: " $( cat /sys/devices/virtual/dmi/id/product_uuid )
    echo "no"
    #exit 1
  fi
fi

# Fallback check of http://169.254.169.254/.
echo "Testing for AWS instance info..."; 
if curl -s -m 1 http://169.254.169.254/latest/dynamic/instance-identity/document | grep -q availabilityZone; then
  echo "yes"
  exit 0
else
  echo "Nope: " $( curl -s -m 1 http://169.254.169.254/latest/dynamic/instance-identity/document )
  echo "no"
  exit 1
fi
