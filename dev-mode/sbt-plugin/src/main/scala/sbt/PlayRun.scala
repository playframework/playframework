/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sbt

import java.nio.file.Files
import java.util.function.Supplier
import java.util.Map as JMap

import scala.annotation.tailrec
import scala.collection.JavaConverters.*
import scala.sys.process.*

import sbt.*
import sbt.util.LoggerContext
import sbt.Keys.*
import sbt.LoggerCompat.*

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.*
import com.typesafe.sbt.packager.Keys.executableScriptName
import com.typesafe.sbt.web.SbtWeb.autoImport.*
import play.runsupport.classloader.AssetsClassLoader
import play.runsupport.CompileResult
import play.runsupport.DevServerRunner
import play.runsupport.DevServerSettings
import play.runsupport.GeneratedSourceMapping
import play.sbt.run.PlayReload
import play.sbt.Colors
import play.sbt.PlayImport.*
import play.sbt.PlayImport.PlayKeys.*
import play.sbt.PlayInteractionMode
import play.sbt.PlayInternalKeys.*
import play.sbt.PlayNonBlockingInteractionMode
import play.sbt.PlayRunHook
import play.sbt.PluginCompat
import play.sbt.PluginCompat.*
import play.sbt.StaticPlayNonBlockingInteractionMode
import play.twirl.compiler.MaybeGeneratedSource
import play.twirl.sbt.SbtTwirl
import xsbti.FileConverter

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

  val playDefaultRunTask = {
    playRunTask(playRunHooks, playDependencyClasspath, playReloaderClasspath, playAssetsClassLoader).map(_ => ())
  }

  private val playDefaultRunTaskNonBlocking =
    playRunTask(
      playRunHooks,
      playDependencyClasspath,
      playReloaderClasspath,
      playAssetsClassLoader,
      Some(StaticPlayNonBlockingInteractionMode)
    )

  val playDefaultBgRunTask =
    playBgRunTask()

  def playRunTask(
      runHooks: TaskKey[Seq[PlayRunHook]],
      dependencyClasspath: TaskKey[Classpath],
      reloaderClasspath: TaskKey[Classpath],
      assetsClassLoader: TaskKey[ClassLoader => ClassLoader],
      interactionMode: Option[PlayInteractionMode] = None
  ): Def.Initialize[InputTask[(PlayInteractionMode, Boolean)]] = Def.inputTask {
    implicit val fc: FileConverter = fileConverter.value
    val args                       = Def.spaceDelimited().parsed

    val state       = Keys.state.value
    val scope       = resolvedScoped.value.scope
    val interaction = interactionMode.getOrElse(playInteractionMode.value)

    val reloadCompile: Supplier[CompileResult] = () => {
      // This code and the below Project.runTask(...) run outside of a user-called sbt command/task.
      // It gets called much later, by code, not by user, when a request comes in which causes Play to re-compile.
      // Since sbt 1.8.0 a LoggerContext closes after command/task that was run by a user is finished.
      // Therefore we need to wrap this code with a new, open LoggerContext.
      // See https://github.com/playframework/playframework/issues/11527
      var loggerContext: LoggerContext = null
      try {
        val newState = interaction match {
          case _: PlayNonBlockingInteractionMode =>
            loggerContext = createLoggerContext(state)
            state.put(Keys.loggerContext, loggerContext)
          case _ => state
        }
        PlayReload.compile(
          () => PluginCompat.runTask(scope / playReload, newState).map(_._2).get,
          () =>
            PluginCompat
              .runTask(scope / reloaderClasspath, newState.put(WebKeys.disableExportedProducts, true))
              .map(_._2)
              .get,
          () => PluginCompat.runTask(scope / streamsManager, newState).map(_._2).get.toEither.right.toOption,
          newState,
          scope
        )
      } finally {
        interaction match {
          case _: PlayNonBlockingInteractionMode => loggerContext.close()
          case _                                 => // no-op
        }
      }

    }

    lazy val devModeServer = DevServerRunner.startDevMode(
      runHooks.value.asJava,
      (Runtime / javaOptions).value.asJava,
      playCommonClassloader.value,
      getFiles(dependencyClasspath.value).asJava,
      reloadCompile,
      cls => assetsClassLoader.value.apply(cls),
      null,
      // avoid monitoring same folder twice or folders that don't exist
      playMonitoredFiles.value.distinct.filter(_.exists()).asJava,
      fileWatchService.value,
      generatedSourceHandlers.asJava,
      playDefaultPort.value,
      playDefaultAddress.value,
      baseDirectory.value,
      devSettings.value.toMap.asJava,
      args.asJava,
      (Compile / run / mainClass).value.get,
      PlayRun
    )

    val serverDidStart = interaction match {
      case nonBlocking: PlayNonBlockingInteractionMode => nonBlocking.start(devModeServer)
      case _                                           =>
        devModeServer

        println()
        println(Colors.green("(Server started, use Enter to stop and go back to the console...)"))
        println()

        try {
          // run mode
          interaction.waitForCancel()
        } finally {
          devModeServer.close()
          println()
        }
        true
    }
    (interaction, serverDidStart)
  }

  def playBgRunTask(): Def.Initialize[InputTask[JobHandle]] = Def.inputTask {
    bgJobService.value.runInBackground(resolvedScoped.value, state.value) { (logger, workingDir) =>
      playDefaultRunTaskNonBlocking.evaluated match {
        case (mode: PlayNonBlockingInteractionMode, serverDidStart) =>
          if (serverDidStart) {
            try {
              Thread.sleep(Long.MaxValue) // Sleep "forever" ;), gets interrupted by "bgStop <id>"
            } catch {
              case _: InterruptedException => mode.stop() // shutdown dev server
            }
          }
      }
    }
  }

  val playPrefixAndAssetsSetting = {
    playPrefixAndAssets := assetsPrefix.value -> (Assets / WebKeys.public).value
  }

  val playAllAssetsSetting = playAllAssets := Seq(playPrefixAndAssets.value)

  val playAssetsClassLoaderSetting = {
    playAssetsClassLoader := uncached {
      val assets =
        playAllAssets.value.map(asset => JMap.entry(asset._1, asset._2))
      parent => new AssetsClassLoader(parent, assets.asJava)
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
    val devSettings = Map.empty[String, String] // there are no dev settings in a prod website

    // Parse HTTP port argument
    val serverSettings = DevServerSettings.parse(
      Seq.empty.asJava,
      filtered.asJava,
      devSettings.asJava,
      extracted.get(playDefaultPort),
      extracted.get(playDefaultAddress)
    )

    require(serverSettings.isAnyPortDefined, "You have to specify https.port when http.port is disabled")

    def fail(state: State) = {
      println()
      println("Cannot start with errors.")
      println()
      state.fail
    }

    Project.runTask(stage, state) match {
      case None                             => fail(state)
      case Some((state, Inc(_)))            => fail(state)
      case Some((state, Value(stagingDir))) =>
        val stagingBin = {
          val path  = (stagingDir / "bin" / extracted.get(executableScriptName)).getAbsolutePath
          val isWin = System.getProperty("os.name").toLowerCase(java.util.Locale.ENGLISH).contains("win")
          if (isWin) s"$path.bat" else path
        }
        val javaOpts = PluginCompat.runTask(Production / javaOptions, state).get._2.toEither.right.getOrElse(Nil)

        // Note that I'm unable to pass system properties along with properties... if I do then I receive:
        //  java.nio.charset.IllegalCharsetNameException: "UTF-8"
        // Things are working without passing system properties, and I'm unsure that they need to be passed explicitly.
        // If def main(args: Array[String]) { problem occurs in this area then at least we know what to look at.
        val args = Seq(stagingBin) ++
          serverSettings.getArgsProperties.asScala.map { e => s"-D${e._1}=${e._2}" } ++
          javaOpts ++
          Seq(s"-Dhttp.port=${serverSettings.getHttpPortOrDisabled}")

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
    val pidFile = Project
      .extract(state)
      .get(Universal / com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.stagingDirectory) / "RUNNING_PID"
    if (pidFile.exists) {
      val pid = IO.read(pidFile)
      s"kill -15 $pid".!
      // PID file will be deleted by a shutdown hook attached on start in ProdServerStart.scala
      println(s"Stopped application with process ID $pid")
    } else println(s"No PID file found at $pidFile. Are you sure the app is running?")
    println()
  }
}
