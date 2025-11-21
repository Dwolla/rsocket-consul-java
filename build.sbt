inThisBuild(List(
  organization := "com.dwolla",
  description := "A library for streaming healthy service instances in Consul to the RSocket Java load balancer.",
  homepage := Some(url("https://github.com/Dwolla/rsocket-consul-java")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  developers := List(
    Developer(
      "dwolla",
      "Dwolla Dev Team",
      "dev+github@dwolla.com",
      url("https://dwolla.com")
    ),
  ),
  startYear := Option(2019),
  autoScalaLibrary := false,
  crossPaths := false,

  tlBaseVersion := "0.0",
  mergifyStewardConfig ~= { _.map {
    _.withAuthor("dwolla-oss-scala-steward[bot]")
      .withMergeMinors(true)
  }},

  githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"), JavaSpec.temurin("17"), JavaSpec.temurin("21")),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowPublishTargetBranches :=
    Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowPublish := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  ),

  tlMimaPreviousVersions += "0.0.6",
))

lazy val `rsocket-consul-java` = (project in file("."))
  .settings(
    Test / fork := true,  // otherwise we get java.net.SocketException: maximum number of DatagramSockets reached
    Compile / doc / javacOptions ~= { _.filterNot(_.startsWith("-X")) },
    libraryDependencies ++= {
      val rsocketVersion = "1.1.5"
      Seq(
        "io.rsocket" % "rsocket-core" % rsocketVersion,
        "io.rsocket" % "rsocket-load-balancer" % rsocketVersion,
        "io.rsocket" % "rsocket-transport-netty" % rsocketVersion,
        "org.asynchttpclient" % "async-http-client" % "3.0.3",
        "com.google.code.gson" % "gson" % "2.13.1",
        "org.slf4j" % "slf4j-api" % "2.0.17",
        "org.junit.jupiter" % "junit-jupiter" % "5.14.1" % Test,
        "org.junit.platform" % "junit-platform-engine" % "1.14.1" % Test,
        "org.junit.platform" % "junit-platform-launcher" % "1.14.1" % Test,
        "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
        "org.mockito" % "mockito-core" % "5.20.0" % Test,
        "org.slf4j" % "slf4j-nop" % "2.0.17" % Test,
      )
    },
  )
