package org.cacique.kafka_happenings

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.stereotype.Component

import java.time.Duration
import java.util
import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap, ExecutorService, Executors, ThreadPoolExecutor, TimeUnit}
import java.util.function.Supplier
import java.util.{Collections, stream}
import akka.event.Logging

class EventSupplier extends Supplier[Option[KafkaEvent]] {
  val queue: util.Queue[Option[KafkaEvent]] = new ArrayBlockingQueue[Option[KafkaEvent]](10000, true)

  override def get(): Option[KafkaEvent] = {
    val value = queue.poll()
    if (value == null) {
      return None
    }
    println(s"Getting message from queue ${value.get.data}")
    value
  }

  def push(e: KafkaEvent): Unit = {
    queue.add(Some(e))
  }
}

case class ConsumerAndStream(consumer: KafkaConsumer[String, String],
                             supplier: EventSupplier,
                             stream: Option[java.util.stream.Stream[Option[KafkaEvent]]],
                             actorRef: ActorRef)

@Component
class KafkaConsumerFactory {
  private val consumersAndStreamsPerClient = new ConcurrentHashMap[String, ConsumerAndStream]()
  private val actorSystem: ActorSystem = ActorSystem()
  private var properties: Properties = Properties(
    List(
      Property("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"),
      Property("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    )
  )

  private val thirtySecondRefresher = Executors.newScheduledThreadPool(1)

  thirtySecondRefresher.scheduleAtFixedRate(() => {
    consumersAndStreamsPerClient.values().forEach(_.supplier.push(KafkaEvent("-1", "{\"event\":\"noEvent\"}")))
  }, 10, 10, TimeUnit.SECONDS)

  def executeConsumer(properties: Properties, topic: String, clientId: String, offset: String): Option[java.util.stream.Stream[Option[KafkaEvent]]] = {
    val stream = if (consumersAndStreamsPerClient.get(clientId) != null) {
      println(s"Same client detected ${clientId}. Will kill current stream and return a new one")
      val supplier = consumersAndStreamsPerClient.get(clientId).supplier
      consumersAndStreamsPerClient.get(clientId).stream.foreach(_.close())
      val newStream = Some(java.util.stream.Stream.generate(supplier))
      consumersAndStreamsPerClient.put(clientId, consumersAndStreamsPerClient.get(clientId).copy(stream = newStream))
      newStream
    } else {
      println("WIll create a new consumer")
      this.properties = Properties(this.properties.properties.appendedAll(properties.properties))
      val consumer = Some(new KafkaConsumer[String, String](this.properties.asJavaProperties()))
      consumer.map { consumer =>
        consumer.subscribe(Collections.singletonList(topic))
        val supplier = new EventSupplier()
        val stream = consume(consumer, supplier)
        consumersAndStreamsPerClient.put(clientId, ConsumerAndStream(consumer, supplier, stream._2, stream._1))
        stream._2
      }.flatten
    }
    if (!offset.isEmpty) {
      val clientData = consumersAndStreamsPerClient.get(clientId)
      clientData.actorRef ! OffsetChange(clientData.consumer, offset.toLong)
    }
    stream
  }

  private def consume(consumer: KafkaConsumer[String, String], supplier: EventSupplier): Tuple2[ActorRef, Option[java.util.stream.Stream[Option[KafkaEvent]]]] = {

    val stream = java.util.stream.Stream.generate(supplier)

    val actor: ActorRef = actorSystem.actorOf(Props[ConsumerActor])

    actor ! Consume(consumer, supplier)

    (actor, Some(stream))
  }
}

case class Consume(consumer: KafkaConsumer[String, String], supplier: EventSupplier)

case class OffsetChange(consumer: KafkaConsumer[String, String], offset: Long)

class ConsumerActor extends Actor {
  override def receive: Receive = {
    case Consume(consumer, supplier) => {
      val timeout = Duration.ofMillis(100)
      try {
        val records: ConsumerRecords[String, String] = consumer.poll(timeout)

        records.forEach { record =>
          supplier.push(KafkaEvent(record.offset().toString, record.value()))
          println(s"Topic ${record.topic()}, Partition: ${record.partition()}, Offset: ${record.offset()}, Key: ${record.key()}, Value: ${record.value()}")
        }
        self ! Consume(consumer, supplier)
      } catch {
        case _: WakeupException => {
          None
        }
        case e: Exception=> {
          e.printStackTrace()
        }
      } finally {
//        consumer.close()
      }
    }
    case OffsetChange(consumer, offset) => {
      consumer.assignment().forEach { partition =>
        consumer.seek(partition, offset)
      }
    }
  }
}