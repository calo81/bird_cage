import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}
import org.cacique.kafka_happenings.{KafkaConsumerFactory, Properties, Property, TopicFactory}

class KafkaTopicTest extends munit.FunSuite {
  test("example kafka consumer") {
    val properties = Properties(List(
      Property(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    )
    )
    TopicFactory.createTopic(topicName = "topic2", numPartitions = 3, replicationFactor = 1, properties = properties, topicProperties = Properties(List()))
  }
}