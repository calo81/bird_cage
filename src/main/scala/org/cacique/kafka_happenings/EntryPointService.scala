package org.cacique.kafka_happenings

import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service

trait EntryPointService {

}

@Service
class EntryPointServiceImpl(@Value("${kafka.url}") val kafkaUrl: String) extends EntryPointService
  with KafkaClusterService
  with CharacterRepo {

  println(s"KAFKA URL ${kafkaUrl}")
}
