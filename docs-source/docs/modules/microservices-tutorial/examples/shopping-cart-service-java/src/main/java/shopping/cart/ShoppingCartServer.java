package shopping.cart;

import akka.actor.typed.ActorSystem;
import akka.grpc.ServiceDescription;
import akka.grpc.javadsl.ServiceHandler;
import akka.grpc.javadsl.ServerReflection;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.Function;
import shopping.cart.proto.ShoppingCartServiceHandlerFactory;
import shopping.cart.proto.ShoppingCartService;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletionStage;

public final class ShoppingCartServer {

    private ShoppingCartServer() { }

    static void start(String host, int port, ActorSystem<?> system, ItemPopularityRepository itemPopularityRepository) {
        ShoppingCartServiceImpl impl = new ShoppingCartServiceImpl(system, itemPopularityRepository);
        @SuppressWarnings("unchecked")
        Function<HttpRequest, CompletionStage<HttpResponse>> service =
                ServiceHandler.concatOrNotFound(
                    ShoppingCartServiceHandlerFactory.create(impl, system),
                    // ServerReflection enabled to support grpcurl without import-path and proto parameters
                    ServerReflection.create(Collections.singletonList(ShoppingCartService.description), system));

        CompletionStage<ServerBinding> bound = Http.get(system).newServerAt(host, port)
                .bind(service::apply);

        bound.whenComplete((binding, ex) -> {
            if (binding != null) {
                binding.addToCoordinatedShutdown(Duration.ofSeconds(3), system);
                InetSocketAddress address = binding.localAddress();
                system.log().info("Shopping online at gRPC server {}:{}", address.getHostString(), address.getPort());
            } else {
                system.log().error("Failed to bind gRPC endpoint, terminating system", ex);
                system.terminate();
            }
        });
    }
}
