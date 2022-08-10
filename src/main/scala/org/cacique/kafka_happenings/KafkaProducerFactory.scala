package org.cacique.kafka_happenings

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.springframework.stereotype.Component

@Component
class KafkaProducerFactory {
  private var producer: Option[KafkaProducer[String, String]] = None
  private var properties: Properties = Properties(
    List(
      Property("key.serializer", "org.apache.kafka.common.serialization.StringSerializer"),
      Property("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    )
  )

  def setupProducer(properties: Properties): KafkaProducerFactory = {
    this.properties = Properties(this.properties.properties.appendedAll(properties.properties))
    producer.foreach(_.close())
    producer = Some(new KafkaProducer[String, String](this.properties.asJavaProperties()))
    this

  }

  def produce(topic: String, message: String): Unit = {
   this
     .producer
     .map(_.send(new ProducerRecord(topic, message)))
     .map(_.get())
     .foreach(println)
  }
}
