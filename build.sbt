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

  githubWorkflowJavaVersions := Seq("adopt@1.8", "adopt@1.11"),
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
))

lazy val `rsocket-consul-java` = (project in file("."))
  .settings(
    Test / fork := true,  // otherwise we get java.net.SocketException: maximum number of DatagramSockets reached
    libraryDependencies ++= {
      val rsocketVersion = "1.1.0"
      Seq(
        "io.rsocket" % "rsocket-core" % rsocketVersion,
        "io.rsocket" % "rsocket-load-balancer" % rsocketVersion,
        "io.rsocket" % "rsocket-transport-netty" % rsocketVersion,
        "org.asynchttpclient" % "async-http-client" % "2.8.1",
        "com.google.code.gson" % "gson" % "2.8.5",
        "org.slf4j" % "slf4j-api" % "1.7.26",
        "org.junit.jupiter" % "junit-jupiter" % "5.4.2" % Test,
        "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
        "org.mockito" % "mockito-core" % "2.27.0" % Test,
        "org.slf4j" % "slf4j-nop" % "1.7.36" % Test,
      )
    },
  )
