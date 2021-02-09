## Running the sample code

1. Start a first node:

    ```shell
    sbt -Dconfig.resource=local1.conf run
    ```

2. Try it with [grpcurl](https://github.com/fullstorydev/grpcurl):

    ```shell
    # add item to cart
    grpcurl -d '{"cartId":"cart1", "itemId":"socks", "quantity":3}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.AddItem
    ```
