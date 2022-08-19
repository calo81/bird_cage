package org.cacique.kafka_happenings

import akka.NotUsed
import akka.stream.scaladsl.Source

case class RequestContext(entryPointService: EntryPointServiceImpl, cookieValue: String) {
  def eventStream: Source[KafkaEvent, NotUsed] = {
    entryPointService.eventStream(cookieValue)
  }
}