package org.cacique.kafka_happenings

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer

import java.time.Duration
import java.util.Collections

object KafkaConsumerFactory {
  private var consumer: Option[KafkaConsumer[String, String]] = None
  private var properties: Properties = Properties(
    List(
      Property("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
      Property("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    )
  )

  def executeConsumer(properties: Properties, topic: String): Unit = {
    this.properties = Properties(this.properties.properties.appendedAll(properties.properties))
    consumer.foreach(_.wakeup())
    consumer = Some(new KafkaConsumer[String, String](this.properties.asJavaProperties()))
    consumer.foreach { consumer =>
      consumer.subscribe(Collections.singletonList(topic))
      consume(consumer)
    }

  }

  private def consume(consumer: KafkaConsumer[String, String]): Unit = {
    val timeout = Duration.ofMillis(100)

    try {
      while (true) {
        val records = consumer.poll(timeout)

        records.forEach { record =>
          println(s"Topic ${record.topic()}, Partition: ${record.partition()}, Offset: ${record.offset()}, Key: ${record.key()}, Value: ${record.value()}")
        }
      }
    } catch {
      case _: WakeupException =>
    } finally {
      consumer.close()
    }

  }
}
