#!/usr/bin/env bash

declare -r tutorial_sources="${PWD}/docs-source/docs/modules/microservices-tutorial/examples"
declare -r tutorial_attachments="${PWD}/docs-source/docs/modules/microservices-tutorial/assets/attachments"

declare -r temporal_folder="${PWD}/target/zips"

mkdir -p ${tutorial_attachments}

## Remove Antora tags from codebase
function removeTags() {
   pushd ${tutorial_sources}
   
   ## remove tags from code
   find . -type f -print0 | xargs -0 sed -i "s/\/\/ tag::[^\[]*\[.*\]//g" 
   find . -type f -print0 | xargs -0 sed -i "s/\/\/ end::[^\[]*\[.*\]//g" 
   
   ## remove tags from config
   find . -type f -print0 | xargs -0 sed -i "s/# tag::[^\[]*\[.*\]//g" 
   find . -type f -print0 | xargs -0 sed -i "s/# end::[^\[]*\[.*\]//g" 

   ## remove call-outs
   find . -type f -print0 | xargs -0 sed -i "s/\/\/ <[0-9]*>//g" 
   
   popd
}


## Cleanup the temporal folder from previous executions
function prepareTemporalFolder() {
   rm -rf ${temporal_folder}
   mkdir -p ${temporal_folder}
}

## Copy a folder with some code into the temporal folder. The 
## copied folder will be renamed to the folder name we want the 
## user to see when unzipping the file.
##   source_name -> subfolder in `microservices-tutorial/examples`
##   target_name ->  folder name the user should see (must not use a numeric prefix of a laguage suffix)
function fetchProject() {
   source_name=$1
   target_name=$2
   cp -a ${tutorial_sources}/${source_name} ${temporal_folder}/${target_name}
}

## Zip the contents in $temporal_folder and create the 
## attachment file (aka, the ZIP file on the appropriate location)
function zipAndAttach() {
   zip_name=$1
   pushd ${temporal_folder}
   zip -r ${tutorial_attachments}/${zip_name} *
   popd
}

## Remove the tags used by Antora snippets from 
## the codebase before zipping
removeTags


## empty codebase (this is a backup option for users 
## that can't use the .g8 template)
prepareTemporalFolder
fetchProject 00-shopping-cart-service-scala shopping-cart-service
zipAndAttach step-2.zip

## gRPC service
prepareTemporalFolder
fetchProject 01-shopping-cart-service-scala shopping-cart-service
zipAndAttach step-3.zip

## basic entity
prepareTemporalFolder
fetchProject 02-shopping-cart-service-scala shopping-cart-service
zipAndAttach step-4.zip

## complete entity
prepareTemporalFolder
fetchProject 03-shopping-cart-service-scala shopping-cart-service
zipAndAttach step-5.zip

## projection query
prepareTemporalFolder
fetchProject 04-shopping-cart-service-scala shopping-cart-service
zipAndAttach step-6.zip

## projection kafka
prepareTemporalFolder
fetchProject 05-shopping-cart-service-scala shopping-cart-service
fetchProject shopping-analytics-service-scala shopping-analytics-service
zipAndAttach step-7.zip

## complete
prepareTemporalFolder
fetchProject shopping-cart-service-scala shopping-cart-service
fetchProject shopping-analytics-service-scala shopping-analytics-service
fetchProject shopping-order-service-scala shopping-order-service-scala
zipAndAttach step-8.zip
