import sbt._
import Keys._

object MinimalBuild extends Build {
  
  lazy val root = Project(id = "minimal", base = file("."), settings = Project.defaultSettings).settings(
    resolvers += Resolver.url("Play", url("http://download.playframework.org/ivy-releases/"))(Resolver.ivyStylePatterns),
    libraryDependencies += "play" %% "play" % "2.0-beta",
    mainClass in (Compile, run) := Some("play.core.server.NettyServer")
  )
  
} 
