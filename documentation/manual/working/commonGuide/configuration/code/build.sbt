// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#play-ws-cache-deps
libraryDependencies += ws
libraryDependencies += ehcache
//#play-ws-cache-deps

//#prefix-with-play-akka-dev-mode
PlayKeys.devSettings += "play.akka.dev-mode.akka.cluster.log-info" -> "off"
//#prefix-with-play-akka-dev-mode

//#custom-akka-http-server-provider
PlayKeys.devSettings += "play.server.provider" -> "server.CustomAkkaHttpServerProvider"
//#custom-akka-http-server-provider
