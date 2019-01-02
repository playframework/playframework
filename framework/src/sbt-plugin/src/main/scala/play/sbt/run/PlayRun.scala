/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import annotation.tailrec

import sbt._
import sbt.Keys._

import play.dev.filewatch.{ SourceModificationWatch => PlaySourceModificationWatch, WatchState => PlayWatchState }

import play.sbt._
import play.sbt.PlayImport._
import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayInternalKeys._
import play.sbt.Colors
import play.core.BuildLink
import play.runsupport.{ AssetsClassLoader, Reloader }
import play.runsupport.Reloader.GeneratedSourceMapping
import play.twirl.compiler.MaybeGeneratedSource
import play.twirl.sbt.SbtTwirl

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.Keys.executableScriptName
import com.typesafe.sbt.web.SbtWeb.autoImport._

/**
 * Provides mechanisms for running a Play application in SBT
 */
object PlayRun extends PlayRunCompat {

  class TwirlSourceMapping extends GeneratedSourceMapping {
    def getOriginalLine(generatedSource: File, line: Integer): Integer = {
      MaybeGeneratedSource.unapply(generatedSource).map(_.mapLine(line): java.lang.Integer).orNull
    }
  }

  /**
   * Configuration for the Play docs application's dependencies. Used to build a classloader for
   * that application. Hidden so that it isn't exposed when the user application is published.
   */
  val DocsApplication = config("docs").hide

  val twirlSourceHandler = new TwirlSourceMapping()

  val generatedSourceHandlers = SbtTwirl.defaultFormats.map{ case (k, v) => ("scala." + k, twirlSourceHandler) }

  val playDefaultRunTask = playRunTask(playRunHooks, playDependencyClasspath,
    playReloaderClasspath, playAssetsClassLoader)

  /**
   * This method is public API, used by sbt-echo, which is used by Activator:
   *
   * https://github.com/typesafehub/sbt-echo/blob/v0.1.3/play/src/main/scala-sbt-0.13/com/typesafe/sbt/echo/EchoPlaySpecific.scala#L20
   *
   * Do not change its signature without first consulting the Activator team.  Do not change its signature in a minor
   * release.
   */
  def playRunTask(
    runHooks: TaskKey[Seq[play.sbt.PlayRunHook]],
    dependencyClasspath: TaskKey[Classpath],
    reloaderClasspath: TaskKey[Classpath],
    assetsClassLoader: TaskKey[ClassLoader => ClassLoader]
  ): Def.Initialize[InputTask[Unit]] = Def.inputTask {

    val args = Def.spaceDelimited().parsed

    val state = Keys.state.value
    val scope = resolvedScoped.value.scope
    val interaction = playInteractionMode.value

    val reloadCompile = () => PlayReload.compile(
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
      playMonitoredFiles.value,
      fileWatchService.value,
      generatedSourceHandlers,
      playDefaultPort.value,
      playDefaultAddress.value,
      baseDirectory.value,
      devSettings.value,
      args,
      (mainClass in (Compile, Keys.run)).value.get,
      PlayRun
    )

    interaction match {
      case nonBlocking: PlayNonBlockingInteractionMode =>
        nonBlocking.start(devModeServer)
      case blocking =>
        devModeServer

        println()
        println(Colors.green("(Server started, use Enter to stop and go back to the console...)"))
        println()

        val maybeContinuous: Option[Watched] = watchContinuously(state, Keys.sbtVersion.value)

        maybeContinuous match {
          case Some(watched) =>
            // ~ run mode
            interaction doWithoutEcho {
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
  @tailrec
  private def twiddleRunMonitor(watched: Watched, state: State, reloader: BuildLink, ws: Option[PlayWatchState] = None): Unit = {
    val ContinuousState = AttributeKey[PlayWatchState]("watch state", "Internal: tracks state for continuous execution.")
    def isEOF(c: Int): Boolean = c == 4

    @tailrec def shouldTerminate: Boolean = (System.in.available > 0) && (isEOF(System.in.read()) || shouldTerminate)

    val sourcesFinder: PlaySourceModificationWatch.PathFinder = getSourcesFinder(watched, state)
    val watchState = ws.getOrElse(state get ContinuousState getOrElse PlayWatchState.empty)

    val (triggered, newWatchState, newState) =
      try {
        val (triggered: Boolean, newWatchState: PlayWatchState) = PlaySourceModificationWatch.watch(sourcesFinder, getPollInterval(watched), watchState)(shouldTerminate)
        (triggered, newWatchState, state)
      } catch {
        case e: Exception =>
          val log = state.log
          log.error("Error occurred obtaining files to watch.  Terminating continuous execution...")
          log.trace(e)
          (false, watchState, state.fail)
      }

    if (triggered) {
      //Then launch compile
      Project.synchronized {
        val start = System.currentTimeMillis
        Project.runTask(compile in Compile, newState).get._2.toEither.right.map { _ =>
          val duration = System.currentTimeMillis - start
          val formatted = duration match {
            case ms if ms < 1000 => ms + "ms"
            case seconds => (seconds / 1000) + "s"
          }
          println("[" + Colors.green("success") + "] Compiled in " + formatted)
        }
      }

      // Avoid launching too much compilation
      sleepForPoolDelay

      // Call back myself
      twiddleRunMonitor(watched, newState, reloader, Some(newWatchState))
    } else {
      ()
    }
  }

  val playPrefixAndAssetsSetting = playPrefixAndAssets := {
    assetsPrefix.value -> (WebKeys.public in Assets).value
  }

  val playAllAssetsSetting = playAllAssets := Seq(playPrefixAndAssets.value)

  val playAssetsClassLoaderSetting = playAssetsClassLoader := {
    val playAllAssetsValue = playAllAssets.value
    parent => new AssetsClassLoader(parent, playAllAssetsValue)
  }

  val playRunProdCommand = Command.args("runProd", "<port>")(testProd)

  val playTestProdCommand = Command.args("testProd", "<port>") { (state: State, args: Seq[String]) =>
    state.log.warn("The testProd command is deprecated, and will be removed in a future version of Play.")
    state.log.warn("To test your application using production mode, run 'runProd' instead.")
    testProd(state, args)
  }

  val playStartCommand = Command.args("start", "<port>") { (state: State, args: Seq[String]) =>
    state.log.warn("The start command is deprecated, and will be removed in a future version of Play.")
    state.log.warn("To run Play in production mode, run 'stage' instead, and then execute the generated start script in target/universal/stage/bin.")
    state.log.warn("To test your application using production mode, run 'runProd' instead.")

    testProd(state, args)
  }

  private def testProd(state: State, args: Seq[String]): State = {

    val extracted = Project.extract(state)

    val interaction = extracted.get(playInteractionMode)
    val noExitSbt = args.contains("--no-exit-sbt")

    val filter = Set("--no-exit-sbt")
    val filtered = args.filterNot(filter)
    val devSettings = Seq.empty[(String, String)] // there are no dev settings in a prod website

    // Parse HTTP port argument
    val (properties, httpPort, httpsPort, httpAddress) = Reloader.filterArgs(filtered, extracted.get(playDefaultPort), extracted.get(playDefaultAddress), devSettings)
    require(httpPort.isDefined || httpsPort.isDefined, "You have to specify https.port when http.port is disabled")

    Project.runTask(stage, state).get._2.toEither match {
      case Left(_) =>
        println()
        println("Cannot start with errors.")
        println()
        state.fail
      case Right(_) =>
        val stagingBin = Some(extracted.get(stagingDirectory in Universal) / "bin" / extracted.get(executableScriptName)).map {
          f =>
            if (System.getProperty("os.name").toLowerCase(java.util.Locale.ENGLISH).contains("win")) f.getAbsolutePath + ".bat" else f.getAbsolutePath
        }.get
        val javaProductionOptions = Project.runTask(javaOptions in Production, state).get._2.toEither.right.getOrElse(Seq[String]())

        // Note that I'm unable to pass system properties along with properties... if I do then I receive:
        //  java.nio.charset.IllegalCharsetNameException: "UTF-8"
        // Things are working without passing system properties, and I'm unsure that they need to be passed explicitly. If def main(args: Array[String]){
        // problem occurs in this area then at least we know what to look at.
        val args = Seq(stagingBin) ++
          properties.map {
            case (key, value) => s"-D$key=$value"
          } ++
          javaProductionOptions ++
          Seq("-Dhttp.port=" + httpPort.getOrElse("disabled"))
        new Thread {
          override def run(): Unit = {
            if (noExitSbt) {
              createAndRunProcess(args)
            } else {
              System.exit(createAndRunProcess(args))
            }
          }
        }.start()

        println(Colors.green(
          """|
            |(Starting server. Type Ctrl+D to exit logs, the server will remain in background)
            | """.stripMargin))

        interaction.waitForCancel()

        println()

        if (noExitSbt) {
          state
        } else {
          state.copy(remainingCommands = List.empty)
        }
    }

  }

  val playStopProdCommand = Command.args("stopProd", "") { (state: State, args: Seq[String]) =>

    val extracted = Project.extract(state)

    val pidFile = extracted.get(stagingDirectory in Universal) / "RUNNING_PID"
    if (!pidFile.exists) {
      println("No PID file found. Are you sure the app is running?")
    } else {
      val pid = IO.read(pidFile)
      kill(pid)
      // PID file will be deleted by a shutdown hook attached on start in ServerStart.scala
      println(s"Stopped application with process ID $pid")
    }
    println()

    if (args.contains("--no-exit-sbt")) {
      state
    } else {
      state.copy(remainingCommands = List.empty)
    }
  }
}
