package org.cacique.kafka_happenings

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.time.Duration
import java.util
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap, ExecutorService, Executors, ThreadPoolExecutor, TimeUnit}
import java.util.function.Supplier
import java.util.{Collections, stream}


class EventSupplier extends Supplier[Option[KafkaEvent]] {
  val queue: util.Queue[Option[KafkaEvent]] = new ArrayBlockingQueue[Option[KafkaEvent]](10000, true)

  override def get(): Option[KafkaEvent] = {
    val value = queue.poll()
    if (value == null) {
      return None
    }
    println(s"Getting message from queue ${value.get.offset}")
    value
  }

  def push(e: KafkaEvent): Unit = {
    queue.add(Some(e))
  }
}

case class ConsumerAndStream(consumer: KafkaConsumer[String, String], supplier: EventSupplier)

@Component
class KafkaConsumerFactory {
  private val consumersAndStreamsPerClient = new ConcurrentHashMap[String, ConsumerAndStream]()
  private var properties: Properties = Properties(
    List(
      Property("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
      Property("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    )
  )

  private val thirtySecondRefresher = Executors.newScheduledThreadPool(1)

  thirtySecondRefresher.scheduleAtFixedRate(() => {
    consumersAndStreamsPerClient.values().forEach(_.supplier.push(KafkaEvent("-1", "noEvent")))
  }, 10, 10, TimeUnit.SECONDS)

  def executeConsumer(properties: Properties, topic: String, clientId: String): Option[java.util.stream.Stream[Option[KafkaEvent]]] = {
    if (consumersAndStreamsPerClient.get(clientId) != null) {
      val supplier = consumersAndStreamsPerClient.get(clientId).supplier
      return Some(java.util.stream.Stream.generate(supplier))
    }

    println("WIll create a new consumer")
    this.properties = Properties(this.properties.properties.appendedAll(properties.properties))
    val consumer = Some(new KafkaConsumer[String, String](this.properties.asJavaProperties()))
    consumer.map { consumer =>

      consumer.subscribe(Collections.singletonList(topic))
      val supplier = new EventSupplier()
      val stream = consume(consumer, supplier)
      consumersAndStreamsPerClient.put(clientId, ConsumerAndStream(consumer, supplier))
      return stream
    }.flatten

  }

  private def consume(consumer: KafkaConsumer[String, String], supplier: EventSupplier): Option[java.util.stream.Stream[Option[KafkaEvent]]] = {

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
