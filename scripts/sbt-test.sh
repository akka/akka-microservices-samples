#!/usr/bin/env bash

set -x
PROJECT_ROOT=$PWD
DIR=$1

cd ${DIR}

if [ -f docker-compose.yml ]; then
  docker-compose up -d
fi

sbt "test; scalafmtCheckAll"

THE_EXIT_CODE=$?

if [ -f docker-compose.yml ]; then
  docker-compose stop
fi

cd ${PROJECT_ROOT}

exit $THE_EXIT_CODE