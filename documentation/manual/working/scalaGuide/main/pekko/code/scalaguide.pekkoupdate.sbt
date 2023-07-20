// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#pekko-update
// The newer Pekko version you want to use.
val pekkoVersion = "1.0.0"

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
val pekkoHTTPVersion = "1.0.0"

// Pekko HTTP dependency used by Play
libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-http-core" % pekkoHTTPVersion
)
//#pekko-http-update
