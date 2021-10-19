name := "receptionist-pool-repro"

version := "0.1"

scalaVersion := "2.13.6"

val AkkaVersion = "2.6.13"

lazy val receptionist_pool_repro = (project in file("."))
  .settings(
      name := "receptionist_pool_repro",
      // processing configs
      libraryDependencies += "com.typesafe" % "config" % "1.4.1",

      // logging
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",

      // akka
      libraryDependencies ++= Seq(
          "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion withSources(),
      )
  )
