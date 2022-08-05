import org.apache.kafka.clients.consumer.KafkaConsumer
import org.cacique.kafka_happenings.{KafkaConsumerFactory, Properties, Property}

// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
class KafkaConsumerTest extends munit.FunSuite {
  test("example kafka consumer") {
    val properties = Properties(List(
      Property("bootstrap.servers", "localhost:9092"),
      Property("group.id", "test_group")
    )
    )
    KafkaConsumerFactory.executeConsumer(properties, "topic1")
  }
}
