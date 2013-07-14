import sbt._
import Keys._
import com.typesafe.sbt.packager.Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "dist-sample"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    cache
  )

  val distAndUnzipTaskKey = TaskKey[File]("dist-and-unzip")
  val distAndUnzipTask = (dist, target) map {
    (dist, target) =>
      val unzippedFiles = IO.unzip(dist, target)
      val unzippedFilePath = unzippedFiles.head.getCanonicalPath.stripPrefix(target.getCanonicalPath)
      val unzippedFolder = target / (unzippedFilePath.split('/')(1))
      val targetUnzippedFolder = target / "dist"
      IO.move(Seq(unzippedFolder -> targetUnzippedFolder))
      targetUnzippedFolder
  }

  val checkStartScriptTaskKey = TaskKey[Unit]("check-start-script")
  val checkStartScriptTask = (distAndUnzipTaskKey) map {
    (targetUnzippedFolder) =>
      val startScript = targetUnzippedFolder / "bin/dist-sample"
      if (!IO.read(startScript).contains( """app_mainclass="play.core.server.NettyServer"""")) {
        error("Cannot find the declaration of the main class in the script")
      }
  }

  val main = play.Project(appName, appVersion, appDependencies).settings(
    distAndUnzipTaskKey <<= distAndUnzipTask,
    checkStartScriptTaskKey <<= checkStartScriptTask
  )

}
