package sample.shoppingorder

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

import akka.actor.typed.ActorSystem
import akka.grpc.scaladsl.ServerReflection
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse

class ShoppingOrderServer(interface: String, port: Int, system: ActorSystem[_]) {
  private implicit val sys: ActorSystem[_] = system
  private implicit val ec: ExecutionContext = system.executionContext

  def start(): Unit = {

    val service: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(
        proto.ShoppingOrderServiceHandler.partial(new ShoppingOrderServiceImpl),
        // ServerReflection enabled to support grpcurl without import-path and proto parameters
        ServerReflection.partial(List(proto.ShoppingOrderService)))

    val bound =
      Http().newServerAt(interface, port).bind(service).map(_.addToCoordinatedShutdown(3.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Shopping order at gRPC server {}:{}", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind gRPC endpoint, terminating system", ex)
        system.terminate()
    }
  }

}
