//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#enable-http2
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, PlayAkkaHttp2Support)
//#enable-http2

//#manually-select-akka-http
PlayKeys.devSettings += "play.server.provider" -> "play.core.server.AkkaHttpServerProvider"
//#manually-select-akka-http
