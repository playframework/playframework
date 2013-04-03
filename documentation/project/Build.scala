import sbt._
import Keys._
import play.core.PlayVersion

object ApplicationBuild extends Build {

  val main = Project("Play-Documentation", file(".")).settings(
    version := PlayVersion.current,
    scalaVersion := PlayVersion.scalaVersion,
    libraryDependencies ++= Seq(
      "play" %% "play" % PlayVersion.current % "test",
      "play" %% "play-test" % PlayVersion.current % "test"
    ),
    unmanagedSourceDirectories in Test ++= (file("manual") ** "code").get
  )

}
