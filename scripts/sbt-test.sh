#!/usr/bin/env bash

set -x

if [ -f docker-compose.yml ]; then
  docker-compose up -d
fi

sbt "test; scalafmtCheckAll"

if [ -f docker-compose.yml ]; then
  docker-compose stop
fi
