/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbt.forkrun

import sbt._
import sbt.complete.Parser
import sbt.Keys._
import sbt.plugins.{ BackgroundRunPlugin, SerializersPlugin }
import sbt.{ BackgroundJobServiceKeys, SerializersKeys, SendEventServiceKeys }

import play.forkrun.protocol.{ ForkConfig, PlayServerStarted, Serializers }
import play.sbt.run._
import play.sbt.{ run => _, _ }
import play.runsupport.Reloader.CompileResult
import scala.concurrent.duration._

object Import {
  object PlayForkRunKeys {
    val playRun = InputKey[Unit]("play-run", "Play in-process reloading run")
    val playForkRun = InputKey[Unit]("play-fork-run", "Play forked reloading run")
    val playForkOptions = TaskKey[PlayForkOptions]("play-fork-run-options", "Fork run options")
    val playForkLogSbtEvents = SettingKey[Boolean]("Determines whether events from sbt server are logged in fork run")
    val playForkCompileTimeout = SettingKey[Duration]("play-fork-compile-timeout", "Timeout for requested compiles")
    val playForkShutdownTimeout = SettingKey[FiniteDuration]("play-fork-shutdown-timeout", "Timeout for shutdown of forked process before forcibly shutting down")
    val playForkConfig = TaskKey[ForkConfig]("play-fork-config", "All setup settings for forked run")
    val playForkNotifyStart = InputKey[Unit]("play-fork-notify-start", "For notifying sbt with the play server url")
    val playForkStarted = TaskKey[String => Unit]("play-fork-started", "Callback for play server start")
    val playForkReload = TaskKey[CompileResult]("play-fork-reload", "Information needed for forked reloads")
  }
}

object PlayForkRun extends AutoPlugin {

  override def requires = Play && SerializersPlugin && BackgroundRunPlugin

  override def trigger = AllRequirements

  val autoImport = Import

  import Import.PlayForkRunKeys._
  import PlayImport.PlayKeys

  val ForkRun = config("fork-run").hide

  override def projectSettings = Seq(
    ivyConfigurations += ForkRun,
    libraryDependencies += "com.typesafe.play" %% "fork-run" % play.core.PlayVersion.current % ForkRun.name,
    PlaySettings.manageClasspath(ForkRun),

    playRun <<= PlayRun.playDefaultRunTask,
    playForkOptions <<= forkOptionsTask,
    playForkRun <<= forkRunTask,

    run in Compile <<= selectRunTask,
    BackgroundJobServiceKeys.backgroundRun in Compile <<= backgroundForkRunTask,

    playForkLogSbtEvents := true,
    playForkCompileTimeout := 5.minutes,
    playForkShutdownTimeout := 10.seconds,

    playForkConfig <<= forkConfigTask,
    playForkNotifyStart <<= serverStartedTask,
    playForkStarted <<= publishUrlTask,
    playForkReload <<= compileTask,
    SerializersKeys.registeredSerializers ++= Serializers.serializers.map(x => RegisteredSerializer(x.serializer, x.unserializer, x.manifest))
  )

  val allInput: Parser[String] = {
    import sbt.complete.DefaultParsers._
    (token(Space) ~> token(any.*.string, "<arg>")).?.map(_.fold("")(" ".+))
  }

  def selectRunTask = Def.inputTaskDyn[Unit] {
    val input = allInput.parsed
    val forked = (fork in (Compile, run)).value
    val runInput = if (forked) playForkRun else playRun
    runInput.toTask(input)
  }

  def forkOptionsTask = Def.task[PlayForkOptions] {
    PlayForkOptions(
      workingDirectory = baseDirectory.value,
      jvmOptions = (javaOptions in (Compile, run)).value,
      classpath = (managedClasspath in ForkRun).value.files,
      baseDirectory = (baseDirectory in ThisBuild).value,
      configKey = thisProjectRef.value.project + "/" + playForkConfig.key.label,
      logLevel = ((logLevel in (Compile, run)) ?? Level.Info).value,
      logSbtEvents = playForkLogSbtEvents.value,
      shutdownTimeout = playForkShutdownTimeout.value)
  }

  def forkRunTask = Def.inputTask[Unit] {
    val args = Def.spaceDelimited().parsed
    val jobService = BackgroundJobServiceKeys.jobService.value
    val handle = jobService.runInBackgroundThread(resolvedScoped.value, { (_, uiContext) =>
      // use normal task streams log rather than the background run logger
      PlayForkProcess(playForkOptions.value, args, streams.value.log)
    })
    PlayConsoleInteractionMode.waitForCancel()
    jobService.stop(handle)
    jobService.waitFor(handle)
  }

  def backgroundForkRunTask = Def.inputTask[BackgroundJobHandle] {
    val args = Def.spaceDelimited().parsed
    BackgroundJobServiceKeys.jobService.value.runInBackgroundThread(resolvedScoped.value, { (logger, uiContext) =>
      PlayForkProcess(playForkOptions.value, args, logger)
    })
  }

  def forkConfigTask = Def.task[ForkConfig] {
    ForkConfig(
      projectDirectory = baseDirectory.value,
      javaOptions = (javaOptions in Runtime).value,
      dependencyClasspath = PlayInternalKeys.playDependencyClasspath.value.files,
      allAssets = PlayInternalKeys.playAllAssets.value,
      docsClasspath = (managedClasspath in PlayRun.DocsApplication).value.files,
      docsJar = PlayKeys.playDocsJar.value,
      devSettings = PlayKeys.devSettings.value,
      defaultHttpPort = PlayKeys.playDefaultPort.value,
      defaultHttpAddress = PlayKeys.playDefaultAddress.value,
      watchService = ForkConfig.identifyWatchService(PlayKeys.fileWatchService.value),
      monitoredFiles = PlayKeys.playMonitoredFiles.value,
      targetDirectory = target.value,
      pollInterval = pollInterval.value,
      notifyKey = thisProjectRef.value.project + "/" + playForkNotifyStart.key.label,
      reloadKey = thisProjectRef.value.project + "/" + playForkReload.key.label,
      compileTimeout = playForkCompileTimeout.value.toMillis,
      mainClass = (mainClass in (Compile, run)).value.getOrElse("play.core.server.NettyServer")
    )
  }

  def serverStartedTask = Def.inputTask[Unit] {
    val url = allInput.parsed.trim
    playForkStarted.value(url)
  }

  def publishUrlTask = Def.task[String => Unit] { url =>
    SendEventServiceKeys.sendEventService.value.sendEvent(PlayServerStarted(url))(Serializers.playServerStartedPickler)
  }

  def compileTask = Def.task[CompileResult] {
    PlayReload.compile(
      () => PlayInternalKeys.playReload.result.value,
      () => PlayInternalKeys.playReloaderClasspath.result.value,
      () => Option(streamsManager.value))
  }

}
