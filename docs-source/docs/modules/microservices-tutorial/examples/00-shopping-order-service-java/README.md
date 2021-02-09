## Running the sample code

1. Start a first node:

    ```shell
    mvn compile exec:exec -DAPP_CONFIG=local1.conf
    ```

2. (Optional) Start another node with different ports:

    ```shell
    mvn compile exec:exec -DAPP_CONFIG=local2.conf
    ```

3. Check for service readiness

    ```shell
    curl http://localhost:9301/ready
    ```
