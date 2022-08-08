package org.cacique.kafka_happenings

import scala.util.{Failure, Success}
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.slowlog.SlowLog
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import org.cacique.kafka_happenings.{CharacterRepo, CorsSupport, SchemaDefinition}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.{ApplicationContext, ApplicationContextAware}
import org.springframework.stereotype.Component
import sangria.marshalling.circe._

import javax.annotation.PostConstruct

// This is the trait that makes `graphQLPlayground and prepareGraphQLRequest` available
import sangria.http.akka.circe.CirceHttpSupport

@SpringBootApplication
class MainApp

object Main extends App {
  val printingHook = new Thread(() => System.out.println("In the middle of a shutdown"))
  Runtime.getRuntime.addShutdownHook(printingHook)
  println("Starting the Application")
  SpringApplication.run(classOf[MainApp])
  while (true) {
    println("heartbeat!")
    try{
      Thread.sleep(60000)
    }catch {
      case e: InterruptedException => {
        println("Shutdown executed")
        System.exit(0)
      }
    }
  }
}

@Component
class Server(@Autowired service: EntryPointServiceImpl) extends CorsSupport with CirceHttpSupport {
  var applicationContext: ApplicationContext = null

  implicit val system: ActorSystem = ActorSystem("sangria-server")

  import system.dispatcher

  def topService(): EntryPointService = {
    applicationContext.getBean[EntryPointService](classOf[EntryPointService])
  }

  @PostConstruct
  def run(): Unit = {
    val route: Route =
      optionalHeaderValueByName("X-Apollo-Tracing") { tracing =>
        path("graphql") {
          graphQLPlayground ~
            prepareGraphQLRequest {
              case Success(req) =>
                val middleware = SlowLog.apolloTracing :: Nil
                val deferredResolver = DeferredResolver.fetchers(SchemaDefinition.characters)
                val graphQLResponse = Executor.execute(
                  schema = SchemaDefinition.StarWarsSchema,
                  queryAst = req.query,
                  userContext = service,
                  variables = req.variables,
                  operationName = req.operationName,
                  middleware = middleware,
                  deferredResolver = deferredResolver
                ).map(OK -> _)
                  .recover {
                    case error: QueryAnalysisError => BadRequest -> error.resolveError
                    case error: ErrorWithResolver => InternalServerError -> error.resolveError
                  }
                complete(graphQLResponse)
              case Failure(preparationError) => complete(BadRequest, formatError(preparationError))
            }
        }
      } ~
        (get & pathEndOrSingleSlash) {
          redirect("/graphql", PermanentRedirect)
        }


    val PORT = sys.props.get("http.port").fold(8282)(_.toInt)
    val INTERFACE = "0.0.0.0"
    Http().newServerAt(INTERFACE, PORT).bindFlow(corsHandler(route))
  }

}
