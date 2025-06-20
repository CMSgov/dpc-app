#!/bin/bash

# Create empty maven.config
mkdir -p ./.mvn
: > ./.mvn/maven.config

function process_line() {
    # Don't process an empty line
    if [ -z "$1" ]
    then
      return
    fi

  KEY="$(echo "$1" | cut -d '=' -f1)"
  VALUE="$(echo "$1" | cut -d '=' -f2)"

  # Quoted values need special handling
  # Before: -DKey="Value"
  # After: "-DKey=Value"
  if [ "${VALUE:0:1}" == '"' ]
  then
      RESULT="\"-D${KEY}=${VALUE/\"}"
  else
      RESULT="-D${KEY}=${VALUE}"
  fi

  echo "$RESULT" >> ./.mvn/maven.config
}

# Loop through decrypted local.env and build maven.config
while read -r LINE; do
  process_line "$LINE";
done < ./ops/config/decrypted/local.env

# Read skips the last line since it doesn't end with a \n
process_line "$LINE"
