// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#pekko-update
// The newer Pekko version you want to use.
val pekkoVersion = "2.6.21"

// Pekko dependencies used by Play
libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor"                 % pekkoVersion,
  "org.apache.pekko" %% "pekko-actor-typed"           % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream"                % pekkoVersion,
  "org.apache.pekko" %% "pekko-slf4j"                 % pekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  // Only if you are using Pekko Testkit
  "org.apache.pekko" %% "pekko-testkit" % pekkoVersion
)
//#pekko-update

//#pekko-http-update
// The newer Pekko HTTP version you want to use.
val pekkoHTTPVersion = "10.2.10"

// Pekko HTTP dependencies used by Play
libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-http-core" % pekkoHTTPVersion,
  // Add this one if you are using HTTP/2
  // (e.g. with enabled PlayPekkoHttp2Support sbt plugin in build.sbt)
  "org.apache.pekko" %% "pekko-http2-support" % pekkoHTTPVersion
)
//#pekko-http-update

//#pekko-exclude-213artifacts
// ...
// scalaVersion := "3.x.x" // When using Scala 3...
// ...

// ...and if using Pekko HTTP version 10.5.x or newer
// you need to set following setting in your build.sbt file:
PlayKeys.pekkoHttpScala3Artifacts := true
//#pekko-exclude-213artifacts
