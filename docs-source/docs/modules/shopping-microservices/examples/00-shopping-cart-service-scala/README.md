## Running the sample code

1. Start a first node:

    ```
    sbt -Dconfig.resource=local1.conf run
    ```

2. (Optional) Start another node with different ports:

    ```
    sbt -Dconfig.resource=local2.conf run
    ```

3. Check for service readiness

    ```
    curl http://localhost:9101/ready
    ```
