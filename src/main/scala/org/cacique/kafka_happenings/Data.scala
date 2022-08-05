package org.cacique.kafka_happenings

import scala.collection.mutable

case class Property(name: String, value: String)

case class Properties(properties: List[Property]) {
  def asJavaProperties(): java.util.Properties = {
    val javaProperties = new java.util.Properties()
    properties.foreach(p => javaProperties.put(p.name, p.value))
    javaProperties
  }

  def asMap(): Map[String, String] = {
    val map = mutable.Map[String, String]()
    properties.foreach(p => map.put(p.name, p.value))
    map.toMap
  }
}


