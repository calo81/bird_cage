package org.cacique.kafka_happenings

import org.apache.kafka.clients.admin.{AdminClient, CreateTopicsOptions, NewTopic}

import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._

object TopicFactory {
  def createTopic(properties: Properties, topicName: String, numPartitions: Int, replicationFactor: Short, topicProperties: Properties): Unit = {
    val adminClient = AdminClient.create(properties.asJavaProperties())
    val topic = new NewTopic(topicName, numPartitions, replicationFactor)
    topic.configs(topicProperties.asMap().asJava)
    val result = adminClient.createTopics(List(topic).asJavaCollection)
    result.config(topicName).get()
  }
}
