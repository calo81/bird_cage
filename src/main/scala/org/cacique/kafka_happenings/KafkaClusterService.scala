package org.cacique.kafka_happenings

import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import scala.jdk.CollectionConverters._

trait KafkaClusterService {
   val kafkaUrl: String
   def getCluster(): Cluster = {
     val adminClient = getAdminClient()

     val clusterResult = adminClient.describeCluster()


     val clusterId = clusterResult.clusterId().get()
     val nodes = clusterResult.nodes().get()
     val brokers = nodes.stream().map(n => Broker(id=n.id().toString)).toList.asScala.toList

     Cluster(id = clusterId, brokers = brokers, properties = Properties(List()))

   }

  def getTopics(): List[Topic] = {
    val adminClient = getAdminClient()
    val topicListings = adminClient.listTopics().listings().get()
    topicListings
      .stream()
      .map(topicListing => Topic(topicListing.name()))
      .toList
      .asScala
      .toList
  }

  private def getAdminClient(): AdminClient = {
    val properties = new java.util.Properties()
    properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl)

    AdminClient.create(properties)
  }
}
