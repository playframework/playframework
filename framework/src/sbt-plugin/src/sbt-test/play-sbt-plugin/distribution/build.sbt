import com.typesafe.sbt.packager.Keys._

name := "dist-sample"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).addPlugins(PlayScala)

val distAndUnzip = TaskKey[File]("dist-and-unzip")

distAndUnzip := {
  val unzippedFiles = IO.unzip(dist.value, target.value)
  val unzippedFilePath = unzippedFiles.head.getCanonicalPath.stripPrefix(target.value.getCanonicalPath)
  val unzippedFolder = target.value / unzippedFilePath.split('/')(1)
  val targetUnzippedFolder = target.value / "dist"
  IO.move(Seq(unzippedFolder -> targetUnzippedFolder))
  targetUnzippedFolder
}

val unzipProjectJar = TaskKey[Unit]("unzip-project-jar")

unzipProjectJar := {
  IO.unzip(distAndUnzip.value / "lib" / s"${organization.value}.${normalizedName.value}-${version.value}.jar", target.value / "projectJar")
}

val checkStartScript = TaskKey[Unit]("check-start-script")

checkStartScript := {
  val startScript = distAndUnzip.value / "bin/dist-sample"
  if (!IO.read(startScript).contains( """app_mainclass="play.core.server.NettyServer"""")) {
    error("Cannot find the declaration of the main class in the script")
  }
}