#!/usr/bin/env bash

set -x
PROJECT_ROOT=$PWD
DIR=$1

cd ${DIR}

if [ -f docker-compose.yml ]; then
  docker-compose up -d
fi

mvn test spotless:check

if [ -f docker-compose.yml ]; then
  docker-compose stop
fi

cd ${PROJECT_ROOT}
