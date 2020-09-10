## Running the sample code

1. Start a first node:

    ```
    sbt -Dconfig.resource=local1.conf run
    ```

2. (Optional) Start another node with different ports:

    ```
    sbt -Dconfig.resource=local2.conf run
    ```

3. Check the readiness of the nodes

    ```
    curl http://localhost:9201/ready
    curl http://localhost:9202/ready
    ```
