/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import java.nio.file.Files

import scala.annotation.tailrec
import scala.sys.process._

import sbt._
import sbt.Keys._
import sbt.internal.io.PlaySource

import play.dev.filewatch.{ SourceModificationWatch => PlaySourceModificationWatch }
import play.dev.filewatch.{ WatchState => PlayWatchState }

import play.sbt.PlayImport._
import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayInternalKeys._
import play.sbt.PlayNonBlockingInteractionMode
import play.sbt.PlayRunHook
import play.sbt.Colors
import play.core.BuildLink
import play.runsupport.AssetsClassLoader
import play.runsupport.Reloader
import play.runsupport.Reloader.GeneratedSourceMapping
import play.twirl.compiler.MaybeGeneratedSource
import play.twirl.sbt.SbtTwirl

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.Keys.executableScriptName
import com.typesafe.sbt.web.SbtWeb.autoImport._

import sbt.internal.io.PlaySource
import scala.sys.process._

/**
 * Provides mechanisms for running a Play application in sbt
 */
object PlayRun {
  class TwirlSourceMapping extends GeneratedSourceMapping {
    def getOriginalLine(generatedSource: File, line: Integer): Integer = {
      MaybeGeneratedSource.unapply(generatedSource).map(_.mapLine(line): java.lang.Integer).orNull
    }
  }

  val twirlSourceHandler = new TwirlSourceMapping()

  val generatedSourceHandlers = SbtTwirl.defaultFormats.map { case (k, _) => s"scala.$k" -> twirlSourceHandler }

  val playDefaultRunTask =
    playRunTask(playRunHooks, playDependencyClasspath, playReloaderClasspath, playAssetsClassLoader)

  /**
   * This method is public API, used by sbt-echo, which is used by Activator:
   *
   * https://github.com/typesafehub/sbt-echo/blob/v0.1.3/play/src/main/scala-sbt-0.13/com/typesafe/sbt/echo/EchoPlaySpecific.scala#L20
   *
   * Do not change its signature without first consulting the Activator team.  Do not change its signature in a minor
   * release.
   */
  def playRunTask(
      runHooks: TaskKey[Seq[PlayRunHook]],
      dependencyClasspath: TaskKey[Classpath],
      reloaderClasspath: TaskKey[Classpath],
      assetsClassLoader: TaskKey[ClassLoader => ClassLoader]
  ): Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val args = Def.spaceDelimited().parsed

    val state       = Keys.state.value
    val scope       = resolvedScoped.value.scope
    val interaction = playInteractionMode.value

    val reloadCompile = () =>
      PlayReload.compile(
        () => Project.runTask(playReload in scope, state).map(_._2).get,
        () => Project.runTask(reloaderClasspath in scope, state).map(_._2).get,
        () => Project.runTask(streamsManager in scope, state).map(_._2).get.toEither.right.toOption
      )

    lazy val devModeServer = Reloader.startDevMode(
      runHooks.value,
      (javaOptions in Runtime).value,
      playCommonClassloader.value,
      dependencyClasspath.value.files,
      reloadCompile,
      assetsClassLoader.value,
      // avoid monitoring same folder twice or folders that don't exist
      playMonitoredFiles.value.distinct.filter(_.exists()),
      fileWatchService.value,
      generatedSourceHandlers,
      playDefaultPort.value,
      playDefaultAddress.value,
      baseDirectory.value,
      devSettings.value,
      args,
      (mainClass in (Compile, run)).value.get,
      PlayRun
    )

    interaction match {
      case nonBlocking: PlayNonBlockingInteractionMode => nonBlocking.start(devModeServer)
      case _ =>
        devModeServer

        println()
        println(Colors.green("(Server started, use Enter to stop and go back to the console...)"))
        println()

        watchContinuously(state) match {
          case Some(watched) =>
            // ~ run mode
            interaction.doWithoutEcho {
              twiddleRunMonitor(watched, state, devModeServer.buildLink, Some(PlayWatchState.empty))
            }
          case None =>
            // run mode
            interaction.waitForCancel()
        }

        devModeServer.close()
        println()
    }
  }

  /**
   * Monitor changes in ~run mode.
   */
  @tailrec private def twiddleRunMonitor(
      watched: Watched,
      state: State,
      reloader: BuildLink,
      ws: Option[PlayWatchState] = None
  ): Unit = {
    val ContinuousState =
      AttributeKey[PlayWatchState]("watch state", "Internal: tracks state for continuous execution.")

    def isEOF(c: Int): Boolean = c == 4

    @tailrec def shouldTerminate: Boolean =
      (System.in.available > 0) && (isEOF(System.in.read()) || shouldTerminate)

    val sourcesFinder = () => {
      watched.watchSources(state).iterator.flatMap(new PlaySource(_).getPaths).collect {
        case p if Files.exists(p) => better.files.File(p)
      }
    }

    val watchState   = ws.getOrElse(state.get(ContinuousState).getOrElse(PlayWatchState.empty))
    val pollInterval = watched.pollInterval.toMillis.toInt

    val (triggered, newWatchState, newState) =
      try {
        val (triggered, newWatchState) =
          PlaySourceModificationWatch.watch(sourcesFinder, pollInterval, watchState)(shouldTerminate)
        (triggered, newWatchState, state)
      } catch {
        case e: Exception =>
          val log = state.log
          log.error("Error occurred obtaining files to watch.  Terminating continuous execution...")
          log.trace(e)
          (false, watchState, state.fail)
      }

    if (triggered) {
      // Then launch compile
      Project.synchronized {
        val start = System.currentTimeMillis
        Project.runTask(compile in Compile, newState).get._2.toEither.right.map { _ =>
          val duration = System.currentTimeMillis - start match {
            case ms if ms < 1000 => ms + "ms"
            case seconds         => (seconds / 1000) + "s"
          }
          println(s"[${Colors.green("success")}] Compiled in $duration")
        }
      }

      // Avoid launching too much compilation
      Thread.sleep(Watched.PollDelay.toMillis)

      // Call back myself
      twiddleRunMonitor(watched, newState, reloader, Some(newWatchState))
    }
  }

  private def watchContinuously(state: State): Option[Watched] = {
    for {
      watched <- state.get(Watched.Configuration)
      monitor <- state.get(Watched.ContinuousEventMonitor)
      if monitor.state.count > 0 // assume we're in ~ run mode
    } yield watched
  }

  val playPrefixAndAssetsSetting = {
    playPrefixAndAssets := assetsPrefix.value -> (WebKeys.public in Assets).value
  }

  val playAllAssetsSetting = playAllAssets := Seq(playPrefixAndAssets.value)

  val playAssetsClassLoaderSetting = {
    playAssetsClassLoader := {
      val assets = playAllAssets.value
      parent => new AssetsClassLoader(parent, assets)
    }
  }

  val playRunProdCommand = Command.args("runProd", "<port>")(testProd)

  val playTestProdCommand = Command.args("testProd", "<port>") { (state: State, args: Seq[String]) =>
    state.log.warn("The testProd command is deprecated, and will be removed in a future version of Play.")
    state.log.warn("To test your application using production mode, run 'runProd' instead.")
    testProd(state, args)
  }

  val playStartCommand = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>
    state.log.warn("The start command is deprecated, and will be removed in a future version of Play.")
    state.log.warn(
      "To run Play in production mode, run 'stage' instead, and then execute the generated start script in target/universal/stage/bin."
    )
    state.log.warn("To test your application using production mode, run 'runProd' instead.")

    testProd(state, args)
  }

  private def testProd(state: State, args: Seq[String]): State = {
    val extracted = Project.extract(state)

    val interaction = extracted.get(playInteractionMode)
    val noExitSbt   = args.contains("--no-exit-sbt")
    val filtered    = args.filterNot(Set("--no-exit-sbt"))
    val devSettings = Seq.empty[(String, String)] // there are no dev settings in a prod website

    // Parse HTTP port argument
    val (properties, httpPort, httpsPort, _) =
      Reloader.filterArgs(filtered, extracted.get(playDefaultPort), extracted.get(playDefaultAddress), devSettings)
    require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

    def fail(state: State) = {
      println()
      println("Cannot start with errors.")
      println()
      state.fail
    }

    Project.runTask(stage, state) match {
      case None                  => fail(state)
      case Some((state, Inc(_))) => fail(state)
      case Some((state, Value(stagingDir))) =>
        val stagingBin = {
          val path  = (stagingDir / "bin" / extracted.get(executableScriptName)).getAbsolutePath
          val isWin = System.getProperty("os.name").toLowerCase(java.util.Locale.ENGLISH).contains("win")
          if (isWin) s"$path.bat" else path
        }
        val javaOpts = Project.runTask(javaOptions in Production, state).get._2.toEither.right.getOrElse(Nil)

        // Note that I'm unable to pass system properties along with properties... if I do then I receive:
        //  java.nio.charset.IllegalCharsetNameException: "UTF-8"
        // Things are working without passing system properties, and I'm unsure that they need to be passed explicitly.
        // If def main(args: Array[String]) { problem occurs in this area then at least we know what to look at.
        val args = Seq(stagingBin) ++
          properties.map { case (key, value) => s"-D$key=$value" } ++
          javaOpts ++
          Seq(s"-Dhttp.port=${httpPort.getOrElse("disabled")}")

        new Thread {
          override def run(): Unit = {
            val exitCode = args.!
            if (!noExitSbt) System.exit(exitCode)
          }
        }.start()

        val msg =
          """|
             |(Starting server. Type Ctrl+D to exit logs, the server will remain in background)
             | """.stripMargin
        println(Colors.green(msg))

        interaction.waitForCancel()
        println()

        if (noExitSbt) state
        else state.exit(ok = true)
    }
  }

  val playStopProdCommand = Command.args("stopProd", "<args>") { (state, args) =>
    stop(state)
    if (args.contains("--no-exit-sbt")) state else state.copy(remainingCommands = Nil)
  }

  def stop(state: State): Unit = {
    val pidFile = Project.extract(state).get(stagingDirectory in Universal) / "RUNNING_PID"
    if (pidFile.exists) {
      val pid = IO.read(pidFile)
      s"kill -15 $pid".!
      // PID file will be deleted by a shutdown hook attached on start in ProdServerStart.scala
      println(s"Stopped application with process ID $pid")
    } else println(s"No PID file found at $pidFile. Are you sure the app is running?")
    println()
  }
}
