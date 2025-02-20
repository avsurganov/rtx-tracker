ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "RTX-Tracker",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor" % "1.1.3",
      "org.apache.pekko" %% "pekko-http" % "1.1.0",
      "org.apache.pekko" %% "pekko-stream" % "1.1.3",
      "org.jsoup" % "jsoup" % "1.18.3",
      "com.twilio.sdk" % "twilio" % "10.6.9",
      "io.github.cdimascio" % "java-dotenv" % "5.2.2"
    )
  )
