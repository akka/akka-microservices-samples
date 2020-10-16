#!/usr/bin/env bash

declare -r tutorial_root="docs-source/docs/modules/microservices-tutorial/examples"
declare -r howto_root="docs-source/docs/modules/how-to/examples"

# scala only project build files
declare SRC="${tutorial_root}/00-shopping-cart-service-scala/build.sbt"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/.scalafmt.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-scala/
cp ${SRC} ${howto_root}/shopping-cart-service-scala/
cp ${SRC} ${howto_root}/cleanup-dependencies-project/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/project/build.properties"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-scala/project/
cp ${SRC} ${tutorial_root}/shopping-order-service-scala/project/
cp ${SRC} ${howto_root}/shopping-cart-service-scala/project/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/project/plugins.sbt"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/project/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-scala/project/
cp ${SRC} ${tutorial_root}/shopping-order-service-scala/project/
cp ${SRC} ${howto_root}/shopping-cart-service-scala/project/

# java only project build files
declare SRC="${tutorial_root}/00-shopping-cart-service-java/pom.xml"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/


# scala and java project files
# Each time we copy the file from the scala variant. They are language agnostic.
declare SRC="${tutorial_root}/00-shopping-cart-service-scala/docker-compose.yml"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-scala/
cp ${SRC} ${tutorial_root}/shopping-order-service-scala/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-java/
cp ${SRC} ${tutorial_root}/shopping-order-service-java/


declare SRC="${tutorial_root}/00-shopping-cart-service-scala/LICENSE"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-scala/
cp ${SRC} ${tutorial_root}/shopping-order-service-scala/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-java/
cp ${SRC} ${tutorial_root}/shopping-order-service-java/


declare SRC="${tutorial_root}/00-shopping-cart-service-scala/ddl-scripts/create_tables.cql"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/ddl-scripts/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/ddl-scripts/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/ddl-scripts/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/ddl-scripts/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/ddl-scripts/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/ddl-scripts/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/ddl-scripts/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/ddl-scripts/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/ddl-scripts/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/ddl-scripts/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/ddl-scripts/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/ddl-scripts/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/application.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/cluster.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/persistence.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/grpc.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/local1.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/local2.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/resources/


declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/local3.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/logback.xml"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-order-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-order-service-java/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/resources/serialization.conf"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/test/resources/logback-test.xml"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/test/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/test/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/test/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/test/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/test/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/test/resources/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-scala/src/test/resources/
cp ${SRC} ${tutorial_root}/shopping-order-service-scala/src/test/resources/

cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/test/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/test/resources/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/test/resources/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/test/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/test/resources/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/test/resources/
cp ${SRC} ${tutorial_root}/shopping-analytics-service-java/src/test/resources/
cp ${SRC} ${tutorial_root}/shopping-order-service-java/src/test/resources/


# scala source file
declare SRC="${tutorial_root}/00-shopping-cart-service-scala/src/main/scala/shopping/cart/CborSerializable.scala"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/

# java source file
declare SRC="${tutorial_root}/00-shopping-cart-service-java/src/main/java/shopping/cart/CborSerializable.java"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/

# from full example
# 05 = after "Projection publishing to Kafka (shopping-analytics-service)", before "Projection calling gRPC service (shopping-order-service)"
# 04 = after "Projection for queries", before "Projection publishing to Kafka (shopping-analytics-service)"
# 03 = after "Complete Event Sourced entity", before "Projection for queries"
# 02 = after "Event Sourced entity", before "Complete Event Sourced entity"
# 01 = after "First gRPC service", before "Event Sourced entity"
declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/resources/kafka.conf"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/protobuf/ShoppingCartEvents.proto"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/protobuf/
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/protobuf/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/protobuf/ShoppingCartService.proto"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/protobuf/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/protobuf/

cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/protobuf/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/protobuf/

declare SRC="${tutorial_root}/shopping-cart-service-scala/ddl-scripts/create_user_tables.cql"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/ddl-scripts/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/ddl-scripts/

cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/ddl-scripts/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/ddl-scripts/

# scala sources
declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/ItemPopularityProjection.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/ItemPopularityProjectionHandler.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/ItemPopularityRepository.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/PublishEventsProjection.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/PublishEventsProjectionHandler.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/ShoppingCart.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/ShoppingCartServer.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/main/scala/shopping/cart/ShoppingCartServiceImpl.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/main/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/test/scala/shopping/cart/CreateTableTestUtils.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/test/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/test/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/test/scala/shopping/cart/ItemPopularityIntegrationSpec.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/test/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/test/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/test/scala/shopping/cart/ItemPopularityProjectionSpec.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/test/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/test/scala/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-scala/src/test/scala/shopping/cart/ShoppingCartSpec.scala"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-scala/src/test/scala/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-scala/src/test/scala/shopping/cart/

# # java sources
declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/ItemPopularityProjection.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/ItemPopularityProjectionHandler.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/ItemPopularityRepository.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/ItemPopularityRepositoryImpl.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/PublishEventsProjection.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/PublishEventsProjectionHandler.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/ShoppingCart.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/ShoppingCartServer.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/main/java/shopping/cart/ShoppingCartServiceImpl.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/main/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/main/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/test/java/shopping/cart/CreateTableTestUtils.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/test/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/test/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/test/java/shopping/cart/ItemPopularityIntegrationTest.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/test/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/test/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/test/java/shopping/cart/ItemPopularityProjectionTest.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/test/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/test/java/shopping/cart/

declare SRC="${tutorial_root}/shopping-cart-service-java/src/test/java/shopping/cart/ShoppingCartTest.java"
cp ${SRC} ${tutorial_root}/05-shopping-cart-service-java/src/test/java/shopping/cart/
cp ${SRC} ${tutorial_root}/04-shopping-cart-service-java/src/test/java/shopping/cart/

# from 04
declare SRC="${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/local-shared.conf"
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/resources/

declare SRC="${tutorial_root}/04-shopping-cart-service-scala/src/main/resources/application.conf"
cp ${SRC} ${tutorial_root}/03-shopping-cart-service-scala/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/resources/

cp ${SRC} ${tutorial_root}/03-shopping-cart-service-java/src/main/resources/
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/resources/

# from 03
declare SRC="${tutorial_root}/03-shopping-cart-service-scala/src/main/scala/shopping/cart/ShoppingCartServer.scala"
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-scala/src/main/scala/shopping/cart/

declare SRC="${tutorial_root}/03-shopping-cart-service-java/src/main/java/shopping/cart/ShoppingCartServer.java"
cp ${SRC} ${tutorial_root}/02-shopping-cart-service-java/src/main/java/shopping/cart/

# from 02
declare SRC="${tutorial_root}/02-shopping-cart-service-scala/src/main/protobuf/ShoppingCartService.proto"
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-scala/src/main/protobuf/
cp ${SRC} ${tutorial_root}/01-shopping-cart-service-java/src/main/protobuf/
