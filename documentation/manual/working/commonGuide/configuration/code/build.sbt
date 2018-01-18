//#play-ws-cache-deps
libraryDependencies += ws
libraryDependencies += ehcache
//#play-ws-cache-deps

//#prefix-with-play-akka-dev-mode
PlayKeys.devSettings ++= Seq(
  "play.akka.dev-mode.akka.cluster.log-info" -> "off"
)
//#prefix-with-play-akka-dev-mode
