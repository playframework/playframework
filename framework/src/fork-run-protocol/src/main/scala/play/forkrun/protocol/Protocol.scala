/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.forkrun.protocol

import java.io.File
import scala.util.{ Success, Failure }

case class ForkConfig(
  projectDirectory: File,
  javaOptions: Seq[String],
  dependencyClasspath: Seq[File],
  allAssets: Seq[(String, File)],
  docsClasspath: Seq[File],
  docsJar: Option[File],
  devSettings: Seq[(String, String)],
  defaultHttpPort: Int,
  defaultHttpAddress: String,
  watchService: ForkConfig.WatchService,
  monitoredFiles: Seq[String],
  targetDirectory: File,
  pollInterval: Int,
  notifyKey: String,
  reloadKey: String,
  compileTimeout: Long,
  mainClass: String)

object ForkConfig {
  import play.runsupport._

  sealed trait WatchService
  case object DefaultWatchService extends WatchService
  case object JDK7WatchService extends WatchService
  case object JNotifyWatchService extends WatchService
  case class PollingWatchService(pollInterval: Int) extends WatchService

  def identifyWatchService(watchService: FileWatchService): WatchService = watchService match {
    case _: DefaultFileWatchService => DefaultWatchService
    case _: JDK7FileWatchService => JDK7WatchService
    case _: JNotifyFileWatchService => JNotifyWatchService
    case sbt: PollingFileWatchService => PollingWatchService(sbt.pollDelayMillis)
    case optional: OptionalFileWatchServiceDelegate => optional.watchService match {
      case Success(service) => identifyWatchService(service)
      case Failure(_) => DefaultWatchService
    }
    case _ => DefaultWatchService
  }
}

case class PlayServerStarted(url: String)
