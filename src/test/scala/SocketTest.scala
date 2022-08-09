import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.admin.AdminClientConfig
import org.cacique.kafka_happenings.{Properties, Property, TopicFactory}
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
import shapeless.ops.zipper.Get

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}


class SocketTest extends munit.FunSuite {
  test("example kafka consumer") {
    implicit val system: ActorSystem = ActorSystem("sangria-server")
    import system.dispatcher

    val request =
      """{
        |	"variables": {},
        |	"query": "subscription{ kafkaEvents { offset } }"
        |}""".stripMargin

//    val request =
//      """{
//        |	"variables": {},
//        |	"query": "query{ topics {name}}"
//        |}""".stripMargin

    println(request)
    val entity = HttpEntity(ContentTypes.`application/json`, request)

    val response: Future[HttpResponse] = Http()
      .singleRequest(HttpRequest(HttpMethods.POST, uri = Uri("http://localhost:8282/graphql"), entity = entity))

    Await.result(response, 30.seconds)



    response.onComplete{ c=>
      println(c)
    }

      response.flatMap { ee =>
        val a = 1
        println(a)
        Unmarshal(ee).to[Source[ServerSentEvent, NotUsed]]
      }
      .foreach {
        v =>
          val r = "gg"
          println(r)
          v.runForeach(println)
      }
  }
}