package org.cacique.kafka_happenings

import akka.NotUsed

import scala.util.{Failure, Random, Success}
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.slowlog.SlowLog
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import org.cacique.kafka_happenings.{CharacterRepo, CorsSupport, SchemaDefinition}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.{ApplicationContext, ApplicationContextAware}
import org.springframework.stereotype.Component
import sangria.ast.OperationType
import sangria.marshalling.circe._

import javax.annotation.PostConstruct
import scala.util.control.NonFatal
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.headers.{HttpCookiePair, SameSite}
import akka.stream.scaladsl.Source

import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

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
    try {
      Thread.sleep(60000)
    } catch {
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

  implicit val streamMaterializer = ActorMaterializer.create(system)


  @PostConstruct
  def run(): Unit = {
    val middleware = SlowLog.apolloTracing :: Nil
    val deferredResolver = DeferredResolver.fetchers(SchemaDefinition.characters)
    val executor = Executor(
      schema = SchemaDefinition.createSchema(streamMaterializer),
      middleware = middleware,
      deferredResolver = deferredResolver
    )
    val route: Route = {

      optionalHeaderValueByName("X-Apollo-Tracing") { tracing =>
        optionalCookie("customer") { maybeCookie: Option[HttpCookiePair] =>
          val cookie: HttpCookiePair = maybeCookie.getOrElse(HttpCookiePair("customer", Random.alphanumeric.take(10).mkString))
          setCookie(cookie.toCookie().withSameSite(SameSite.None).withSecure(false)) {

            extractRequest { request =>
              val entity: HttpEntity.Strict = request.entity.asInstanceOf[HttpEntity.Strict]
              val body = entity.getData().decodeString(Charset.forName("UTF-8"))
              path("graphql") {
                graphQLPlayground ~
                  prepareGraphQLRequest {
                    case Success(req) =>
                      req.query.operationType(req.operationName) match {
                        case Some(OperationType.Subscription) ⇒
                          import sangria.execution.ExecutionScheme.Stream
                          import sangria.streaming.akkaStreams._

                          val context = RequestContext(service, cookie.value)

                          complete(
                            executor.prepare(req.query, context, (), req.operationName, req.variables)
                              .map { preparedQuery ⇒
                                ToResponseMarshallable(preparedQuery.execute()
                                  .map { result ⇒
//                                    println(s"Found result ${result}")
                                    ServerSentEvent(result.noSpaces, eventType = Some("next"))
                                  }
                                  .recover { case NonFatal(error) ⇒
                                    println(error, "Unexpected error during event stream processing.")
                                    ServerSentEvent(error.getMessage)
                                  })
                              }
                              .recover {
                                case error: QueryAnalysisError ⇒
                                  error.printStackTrace()
                                  ToResponseMarshallable(BadRequest → error.resolveError)
                                case error: ErrorWithResolver ⇒
                                  error.printStackTrace()
                                  ToResponseMarshallable(InternalServerError → error.resolveError)
                              })

                        case _ =>
                          val graphQLResponse = executor.execute(
                            queryAst = req.query,
                            userContext = RequestContext(service, cookie.value),
                            variables = req.variables,
                            operationName = req.operationName,
                            root = ()
                          ).map(OK -> _)
                            .recover {
                              case error: QueryAnalysisError => BadRequest -> error.resolveError
                              case error: ErrorWithResolver => InternalServerError -> error.resolveError
                            }
                          complete(graphQLResponse)
                      }


                    case Failure(preparationError) => complete(BadRequest, formatError(preparationError))
                  }
              }
            }
          }
        }
      } ~
        (get & pathEndOrSingleSlash) {
          redirect("/graphql", PermanentRedirect)
        }
    }


    val PORT = sys.props.get("http.port").fold(8282)(_.toInt)
    val INTERFACE = "0.0.0.0"
    Http().newServerAt(INTERFACE, PORT).bindFlow(corsHandler(route))
  }

}
