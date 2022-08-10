import org.cacique.kafka_happenings.{KafkaConsumerFactory, KafkaProducerFactory, Properties, Property}

class KafkaProducerTest extends munit.FunSuite {
  test("example kafka producer") {
    val properties = Properties(List(
      Property("bootstrap.servers", "localhost:9092"),
    )
    )
    new KafkaProducerFactory().setupProducer(properties).produce("test_topic", "event_ii")
  }
}
