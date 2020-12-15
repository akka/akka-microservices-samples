#!/usr/bin/env bash

set -x

if [ -f docker-compose.yml ]; then
  docker-compose up -d
fi

mvn test spotless:check

if [ -f docker-compose.yml ]; then
  docker-compose stop
fi
