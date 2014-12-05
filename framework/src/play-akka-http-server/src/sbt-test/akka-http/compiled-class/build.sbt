lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "compiled-class"

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

// Change our tests directory because the usual "test" directory clashes
// with the scripted "test" file.
scalaSource in Test <<= baseDirectory(_ / "tests")

libraryDependencies += "com.typesafe.play" %% "play-akka-http-server-experimental" % sys.props("project.version")

libraryDependencies += ws

libraryDependencies += specs2 % Test

mainClass in Compile := Some("play.core.server.akkahttp.AkkaHttpServer")

PlayKeys.playInteractionMode := play.StaticPlayNonBlockingInteractionMode

InputKey[Unit]("verify-resource-contains") := {
  val args = Def.spaceDelimited("<path> <status> <words> ...").parsed
  val path = args.head
  val status = args.tail.head.toInt
  val assertions = args.tail.tail
  DevModeBuild.verifyResourceContains(path, status, assertions, 0)
}
