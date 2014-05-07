import com.typesafe.sbt.packager.Keys._

name := "dist-sample"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

val distAndUnzip = TaskKey[File]("dist-and-unzip")

distAndUnzip := {
  val unzippedFiles = IO.unzip(dist.value, target.value)
  val unzippedFilePath = unzippedFiles.head.getCanonicalPath.stripPrefix(target.value.getCanonicalPath)
  val unzippedFolder = target.value / unzippedFilePath.split('/')(1)
  val targetUnzippedFolder = target.value / "dist"
  IO.move(Seq(unzippedFolder -> targetUnzippedFolder))
  targetUnzippedFolder
}

val checkStartScript = TaskKey[Unit]("check-start-script")

checkStartScript := {
  val startScript = distAndUnzip.value / "bin/dist-sample"
  if (!IO.read(startScript).contains( """app_mainclass="play.core.server.NettyServer"""")) {
    error("Cannot find the declaration of the main class in the script")
  }
}