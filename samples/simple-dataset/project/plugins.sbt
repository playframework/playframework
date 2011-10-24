resolvers ++= Option(System.getProperty("play.home")).map { home =>
    Resolver.file("play-repository", file(home) / "../repository")(Resolver.ivyStylePatterns)
}.toSeq

libraryDependencies += "play" %% "play" % "2.0"