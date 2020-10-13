#!/bin/sh

# abort script if a command fails
set -e

# prepend antora if command is not detected
if [ $# -eq 0 ] || [ "${1:0:1}" == '-' ] || [ -z `command -v "$1" || echo -n` ]; then
  set -- antora "$@"
fi

exec "$@"
