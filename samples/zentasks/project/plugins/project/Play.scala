import sbt._
import Keys._

object Play extends Build {

    val version = "2.0"

    val playRepository = Option(System.getProperty("play.home")).map { home =>
        Resolver.file("play-repository", file(home) / "../repository")(Resolver.ivyStylePatterns)
    }.toSeq

    val play = Project("Play " + version, file(".")).settings(
        libraryDependencies += "play" %% "play" % version,
        resolvers ++= playRepository
    )

}
            