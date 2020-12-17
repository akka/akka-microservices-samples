#!/usr/bin/env bash

declare -r tutorial_sources="${PWD}/docs-source/docs/modules/microservices-tutorial/examples"
declare -r tutorial_attachments="${PWD}/docs-source/docs/modules/microservices-tutorial/assets/attachments"

declare -r temporal_folder="${PWD}/target/zips"
declare -r temporal_attachments="${PWD}/target/microservices-tutorial/_attachments"
mkdir -p ${temporal_attachments}

mkdir -p ${tutorial_attachments}

## Remove the tags used by Antora snippets from 
## the codebase in the current folder
function removeTags() {
   ## remove tags from code
   find . -type f -print0 | xargs -0 sed -i "s/\/\/ tag::[^\[]*\[.*\]//g" 
   find . -type f -print0 | xargs -0 sed -i "s/\/\/ end::[^\[]*\[.*\]//g" 
   
   ## remove tags from config
   find . -type f -print0 | xargs -0 sed -i "s/# tag::[^\[]*\[.*\]//g" 
   find . -type f -print0 | xargs -0 sed -i "s/# end::[^\[]*\[.*\]//g" 

   ## remove call-outs
   find . -type f -print0 | xargs -0 sed -i "s/\/\/ <[0-9]*>//g" 
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
   removeTags
   zip --quiet -r ${tutorial_attachments}/${zip_name} *
   cp ${tutorial_attachments}/${zip_name} ${temporal_attachments}
   popd
}


## Scala Zip files
## empty seed template
prepareTemporalFolder
fetchProject 00-shopping-cart-service-scala shopping-cart-service
fetchProject 00-shopping-analytics-service-scala shopping-analytics-service
fetchProject 00-shopping-order-service-scala shopping-order-service
zipAndAttach 0-shopping-cart-start-scala.zip

## gRPC service
prepareTemporalFolder
fetchProject 01-shopping-cart-service-scala shopping-cart-service
fetchProject 00-shopping-analytics-service-scala shopping-analytics-service
fetchProject 00-shopping-order-service-scala shopping-order-service
zipAndAttach 1-shopping-cart-grpc-scala.zip

## basic entity
prepareTemporalFolder
fetchProject 02-shopping-cart-service-scala shopping-cart-service
fetchProject 00-shopping-analytics-service-scala shopping-analytics-service
fetchProject 00-shopping-order-service-scala shopping-order-service
zipAndAttach 2-shopping-cart-event-sourced-scala.zip

## complete entity
prepareTemporalFolder
fetchProject 03-shopping-cart-service-scala shopping-cart-service
fetchProject 00-shopping-analytics-service-scala shopping-analytics-service
fetchProject 00-shopping-order-service-scala shopping-order-service
zipAndAttach 3-shopping-cart-event-sourced-complete-scala.zip

## projection query
prepareTemporalFolder
fetchProject 04-shopping-cart-service-scala shopping-cart-service
fetchProject 00-shopping-analytics-service-scala shopping-analytics-service
fetchProject 00-shopping-order-service-scala shopping-order-service
zipAndAttach 4-shopping-cart-projection-scala.zip

## projection kafka
prepareTemporalFolder
fetchProject 05-shopping-cart-service-scala shopping-cart-service
fetchProject shopping-analytics-service-scala shopping-analytics-service
fetchProject 00-shopping-order-service-scala shopping-order-service
zipAndAttach 5-shopping-cart-projection-kafka-scala.zip

## complete
prepareTemporalFolder
fetchProject shopping-cart-service-scala shopping-cart-service
fetchProject shopping-analytics-service-scala shopping-analytics-service
fetchProject shopping-order-service-scala shopping-order-service
zipAndAttach 6-shopping-cart-complete-scala.zip


## Java Zip files
## empty seed template
prepareTemporalFolder
fetchProject 00-shopping-cart-service-java shopping-cart-service
fetchProject 00-shopping-analytics-service-java shopping-analytics-service
fetchProject 00-shopping-order-service-java shopping-order-service
zipAndAttach 0-shopping-cart-start-java.zip

## gRPC service
prepareTemporalFolder
fetchProject 01-shopping-cart-service-java shopping-cart-service
fetchProject 00-shopping-analytics-service-java shopping-analytics-service
fetchProject 00-shopping-order-service-java shopping-order-service
zipAndAttach 1-shopping-cart-grpc-java.zip

## basic entity
prepareTemporalFolder
fetchProject 02-shopping-cart-service-java shopping-cart-service
fetchProject 00-shopping-analytics-service-java shopping-analytics-service
fetchProject 00-shopping-order-service-java shopping-order-service
zipAndAttach 2-shopping-cart-event-sourced-java.zip

## complete entity
prepareTemporalFolder
fetchProject 03-shopping-cart-service-java shopping-cart-service
fetchProject 00-shopping-analytics-service-java shopping-analytics-service
fetchProject 00-shopping-order-service-java shopping-order-service
zipAndAttach 3-shopping-cart-event-sourced-complete-java.zip

## projection query
prepareTemporalFolder
fetchProject 04-shopping-cart-service-java shopping-cart-service
fetchProject 00-shopping-analytics-service-java shopping-analytics-service
fetchProject 00-shopping-order-service-java shopping-order-service
zipAndAttach 4-shopping-cart-projection-java.zip

## projection kafka
prepareTemporalFolder
fetchProject 05-shopping-cart-service-java shopping-cart-service
fetchProject shopping-analytics-service-java shopping-analytics-service
fetchProject 00-shopping-order-service-java shopping-order-service
zipAndAttach 5-shopping-cart-projection-kafka-java.zip

## complete
prepareTemporalFolder
fetchProject shopping-cart-service-java shopping-cart-service
fetchProject shopping-analytics-service-java shopping-analytics-service
fetchProject shopping-order-service-java shopping-order-service
zipAndAttach 6-shopping-cart-complete-java.zip
