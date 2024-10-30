#!/bin/bash

# Test if environment is running inside AWS EC2
#
# Josh Kupershmidt (https://serverfault.com/users/223639/josh-kupershmidt) 3/20/2018

# This first, simple check will work for many older instance types.
if [ -f /sys/hypervisor/uuid ]; then
  # File should be readable by non-root users.
  if [ "$(head -c 3 /sys/hypervisor/uuid)" = "ec2" ]; then
    echo "yes"
  else
    echo "no"
  fi

# This check will work on newer m5/c5 instances, but only if you have root!
elif [ -r /sys/devices/virtual/dmi/id/product_uuid ]; then
  # If the file exists AND is readable by us, we can rely on it.
  if [ "$(head -c 3 /sys/devices/virtual/dmi/id/product_uuid)" = "EC2" ]; then
    echo "yes"
  else
    echo "no"
  fi

else
  # Fallback check of http://169.254.169.254/. 
  if curl -s -m 1 http://169.254.169.254/latest/dynamic/instance-identity/document | grep -q availabilityZone; then
    echo "yes"
  else
    echo "no"
  fi
fi
