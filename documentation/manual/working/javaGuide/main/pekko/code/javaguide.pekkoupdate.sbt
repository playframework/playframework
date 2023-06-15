// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#pekko-update
// The newer Pekko version you want to use.
val akkaVersion = "2.6.21"

// Pekko dependencies used by Play
libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor"                 % akkaVersion,
  "org.apache.pekko" %% "pekko-actor-typed"           % akkaVersion,
  "org.apache.pekko" %% "pekko-stream"                % akkaVersion,
  "org.apache.pekko" %% "pekko-slf4j"                 % akkaVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % akkaVersion,
  // Only if you are using Pekko Testkit
  "org.apache.pekko" %% "pekko-testkit" % akkaVersion
)
//#pekko-update

//#pekko-http-update
// The newer Pekko HTTP version you want to use.
val akkaHTTPVersion = "10.2.10"

// Pekko HTTP dependencies used by Play
libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-http-core" % akkaHTTPVersion,
  // Add this one if you are using HTTP/2
  // (e.g. with enabled PlayPekkoHttp2Support sbt plugin in build.sbt)
  "org.apache.pekko" %% "pekko-http2-support" % akkaHTTPVersion
)
//#pekko-http-update

//#pekko-exclude-213artifacts
// ...
// scalaVersion := "3.x.x" // When using Scala 3...
// ...

// ...and if using Pekko HTTP version 10.5.x or newer
// you need to set following setting in your build.sbt file:
PlayKeys.akkaHttpScala3Artifacts := true
//#pekko-exclude-213artifacts
