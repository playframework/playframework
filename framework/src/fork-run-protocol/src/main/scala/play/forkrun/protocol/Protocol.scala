/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrun.protocol

import java.io.File
import scala.util.{ Success, Failure }
import sbt.protocol._

case class ForkConfig(
  projectDirectory: File,
  javaOptions: Seq[String],
  dependencyClasspath: Seq[File],
  allAssets: Seq[(String, File)],
  docsClasspath: Seq[File],
  devSettings: Seq[(String, String)],
  defaultHttpPort: Int,
  watchService: ForkConfig.WatchService,
  monitoredFiles: Seq[String],
  targetDirectory: File,
  pollInterval: Int,
  notifyKey: String,
  reloadKey: String,
  compileTimeout: Long)

object ForkConfig {
  import play.runsupport._

  sealed trait WatchService
  case object DefaultWatchService extends WatchService
  case object JDK7WatchService extends WatchService
  case object JNotifyWatchService extends WatchService
  case class SbtWatchService(pollInterval: Int) extends WatchService

  def identifyWatchService(watchService: PlayWatchService): WatchService = watchService match {
    case _: DefaultPlayWatchService => DefaultWatchService
    case _: JDK7PlayWatchService => JDK7WatchService
    case _: JNotifyPlayWatchService => JNotifyWatchService
    case sbt: SbtPlayWatchService => SbtWatchService(sbt.pollDelayMillis)
    case optional: OptionalPlayWatchServiceDelegate => optional.watchService match {
      case Success(service) => identifyWatchService(service)
      case Failure(_) => DefaultWatchService
    }
    case _ => DefaultWatchService
  }
}

case class PlayServerStarted(url: String)
