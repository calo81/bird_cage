val scala3Version = "2.13.8"
val akkaVersion = "2.6.19"
val circeVersion = "0.14.1"
val sangriaAkkaHttpVersion = "0.0.2"
val springBootVersion = "2.7.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "kafka_happenings",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.2.1",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,


    libraryDependencies ++= Seq(
      "org.sangria-graphql" % "sangria_2.13" % "2.1.3",
      "org.sangria-graphql" % "sangria-slowlog_2.13" % "2.0.2",
      "org.sangria-graphql" % "sangria-circe_2.13" % "1.3.2",

      "org.sangria-graphql" % "sangria-akka-http-core_2.13" % sangriaAkkaHttpVersion,
      "org.sangria-graphql" % "sangria-akka-http-circe_2.13" % sangriaAkkaHttpVersion,

      "com.typesafe.akka" % "akka-http_2.13" % "10.2.4",
      "com.typesafe.akka" % "akka-actor_2.13" % akkaVersion,
      "com.typesafe.akka" % "akka-stream_2.13" % akkaVersion,
      "de.heikoseeberger" % "akka-http-circe_2.13" % "1.36.0",

      "io.circe" % "circe-core_2.13" % circeVersion,
      "io.circe" % "circe-parser_2.13" % circeVersion,
      "io.circe" % "circe-optics_2.13" % circeVersion,

      "org.springframework.boot" % "spring-boot-starter" % springBootVersion,

      "org.scalatest" %% "scalatest" % "3.2.9" % Test
    ),

    scalacOptions := Seq(

    )
  )
