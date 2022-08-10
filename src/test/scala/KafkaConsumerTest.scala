import akka.actor.ActorSystem
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.cacique.kafka_happenings.{EntryPointServiceImpl, KafkaConsumerFactory, Properties, Property}

// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
class KafkaConsumerTest extends munit.FunSuite {
  test("example kafka consumer") {
    val properties = Properties(List(
      Property("bootstrap.servers", "localhost:9092"),
      Property("group.id", "test_group")
    )
    )
    new KafkaConsumerFactory().executeConsumer(properties, "topic1")
  }

  test("example kafka consumer stream source") {
    implicit val system: ActorSystem = ActorSystem("sangria-server")

    import system.dispatcher
    val properties = Properties(List(
      Property("bootstrap.servers", "localhost:9092"),
      Property("group.id", "test_group")
    )
    )
    val service = new EntryPointServiceImpl("localhost:9092",  new KafkaConsumerFactory())

    service.eventStream.runForeach(println)
  }
}
