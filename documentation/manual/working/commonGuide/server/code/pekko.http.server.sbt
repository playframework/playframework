// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#enable-http2
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, PlayPekkoHttp2Support)
//#enable-http2

//#manually-select-pekko-http
PlayKeys.devSettings += "play.server.provider" -> "play.core.server.PekkoHttpServerProvider"
//#manually-select-pekko-http
