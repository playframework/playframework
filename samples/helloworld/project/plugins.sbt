resolvers ++= Seq(
    Resolver.url("Play", url("http://download.playframework.org/ivy-releases/"))(Resolver.ivyStylePatterns),
    Resolver.url("Typesafe Repository", url("http://repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns),
    "Akka Repo" at "http://akka.io/repository"
)

libraryDependencies += "play" %% "play" % "2.0"