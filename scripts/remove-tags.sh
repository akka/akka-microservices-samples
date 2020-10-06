#!/usr/bin/env bash

declare -r tutorial_sources="${PWD}/docs-source/docs/modules/microservices-tutorial/examples"

cd ${tutorial_sources}
find . -type f -print0 | xargs -0 sed -i "s/\/\/ tag::[^\[]*\[.*\]//g" 
find . -type f -print0 | xargs -0 sed -i "s/# tag::[^\[]*\[.*\]//g" 
find . -type f -print0 | xargs -0 sed -i "s/\/\/ end::[^\[]*\[.*\]//g" 
find . -type f -print0 | xargs -0 sed -i "s/# end::[^\[]*\[.*\]//g" 

git diff 