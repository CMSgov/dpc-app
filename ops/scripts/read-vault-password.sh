#!/usr/bin/env bash

if [ ! -z $VAULT_PW ]; then
  echo $VAULT_PW
elif [ -f .vault_password ]; then
  cat .vault_password
fi