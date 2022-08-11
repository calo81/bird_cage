package org.cacique.kafka_happenings

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.time.Duration
import java.util
import java.util.concurrent.{ArrayBlockingQueue, ExecutorService, ThreadPoolExecutor}
import java.util.function.Supplier
import java.util.{Collections, stream}
import java.util.concurrent.Executors


class EventSupplier extends Supplier[Option[KafkaEvent]] {
  val queue: util.Queue[Option[KafkaEvent]] = new ArrayBlockingQueue[Option[KafkaEvent]](10000, true)

  override def get(): Option[KafkaEvent] = {
    val value = queue.poll()
    if(value == null){
      return None
    }
    println(s"Getting message from queue ${value}")
    value
  }

  def push(e: KafkaEvent): Unit = {
    queue.add(Some(e))
  }
}

@Component
class KafkaConsumerFactory {
  private var consumer: Option[KafkaConsumer[String, String]] = None
  private var properties: Properties = Properties(
    List(
      Property("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
      Property("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    )
  )

  def executeConsumer(properties: Properties, topic: String): Option[java.util.stream.Stream[Option[KafkaEvent]]] = {
    this.properties = Properties(this.properties.properties.appendedAll(properties.properties))
    val consumer = Some(new KafkaConsumer[String, String](this.properties.asJavaProperties()))
    consumer.map { consumer =>
      consumer.subscribe(Collections.singletonList(topic))
      consume(consumer)
    }.flatten

  }

  private def consume(consumer: KafkaConsumer[String, String]): Option[java.util.stream.Stream[Option[KafkaEvent]]] = {


    val supplier = new EventSupplier()

    val stream = java.util.stream.Stream.generate(supplier)

    val executor = Executors.newSingleThreadExecutor

    executor.execute(new ConsumerThread(consumer, supplier))

    Some(stream)
  }
}

class ConsumerThread(consumer: KafkaConsumer[String, String], supplier: EventSupplier) extends Runnable {

  def run() {
    val timeout = Duration.ofMillis(100)
    try {
      while (true) {
        val records = consumer.poll(timeout)

        records.forEach { record =>
          supplier.push(KafkaEvent(record.offset().toString, record.value()))
          println(s"Topic ${record.topic()}, Partition: ${record.partition()}, Offset: ${record.offset()}, Key: ${record.key()}, Value: ${record.value()}")
        }
      }
    } catch {
      case _: WakeupException => {
        None
      }
    } finally {
      consumer.close()
    }
  }
}
