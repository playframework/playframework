// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#play-ws-cache-deps
libraryDependencies += ws
libraryDependencies += ehcache
//#play-ws-cache-deps

//#prefix-with-play-pekko-dev-mode
PlayKeys.devSettings += "play.pekko.dev-mode.pekko.cluster.log-info" -> "off"
//#prefix-with-play-pekko-dev-mode

//#custom-pekko-http-server-provider
PlayKeys.devSettings += "play.server.provider" -> "server.CustomPekkoHttpServerProvider"
//#custom-pekko-http-server-provider
