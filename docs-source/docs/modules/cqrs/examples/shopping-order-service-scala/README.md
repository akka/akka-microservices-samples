
## Running the sample code

```
sbt "runMain sample.shoppingorder.Main 4551"
```

Try it with [grpcurl](https://github.com/fullstorydev/grpcurl):

```
grpcurl -d '{"cartId":"cart1", "items":[{"itemId":"socks", "quantity":3}, {"itemId":"t-shirt", "quantity":2}]}' -plaintext 127.0.0.1:9051 shoppingorder.ShoppingOrderService.Order
```
