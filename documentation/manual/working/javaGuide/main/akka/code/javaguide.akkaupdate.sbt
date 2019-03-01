//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

//#akka-update
// The newer Akka version you want to use.
val akkaVersion = "2.5.16"

// Akka dependencies used by Play
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"  % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j"  % akkaVersion,
  // Only if you are using Akka Testkit
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion
)
//#akka-update

//#akka-http-update
// The newer Akka HTTP version you want to use.
val akkaHTTPVersion = "10.1.4"

// Akka HTTP dependencies used by Play
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % akkaHTTPVersion,
  // Add this one if you are using HTTP/2
  "com.typesafe.akka" %% "akka-http2-support" % akkaHTTPVersion
)
//#akka-http-update
