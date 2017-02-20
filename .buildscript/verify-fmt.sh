#!/bin/bash

make fmt

# Validate that there are no changed files after formatting.
if [[ $(git status 2> /dev/null | tail -n1) != "nothing to commit, working directory clean" ]]; then
  echo "Please run `make fmt` before committing."
  exit 1
fi
