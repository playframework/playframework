// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import java.nio.file._

import scala.sys.process.Process

import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping

val transform = taskKey[Pipeline.Stage]("transformer")

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    name          := "assets-pipeline",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    // In sbt 1.4 extraLoggers got deprecated in favor of extraAppenders (new in sbt 1.4) and as a consequence logManager switched to use extraAppenders.
    // To be able to run the tests in sbt 1.2+ however we can't use extraAppenders yet and to run the tests in sbt 1.4+ we need to make logManager use extraLoggers again.
    // https://github.com/sbt/sbt/commit/2e9805b9d01c6345214c14264c61692d9c21651c#diff-6d9589bfb3f1247d2eace99bab7e928590337680d1aebd087d9da286586fba77R455
    logManager := sbt.internal.LogManager.defaults(extraLoggers.value, ConsoleOut.systemOut),
    extraLoggers ~= (fn => BufferLogger +: fn(_)),
    libraryDependencies += guice,
    // can't use test directory since scripted calls its script "test"
    Test / sourceDirectory := baseDirectory.value / "tests",
    Test / scalaSource     := baseDirectory.value / "tests",
    transform := { (mappings: Seq[PathMapping]) =>
      streams.value.log.info("Running transform")
      mappings
    },
    Assets / pipelineStages := Seq(transform),
    InputKey[Unit]("verifyResourceContains") := {
      val args       = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path       = args.head
      val status     = args.tail.head.toInt
      val assertions = args.tail.tail
      ScriptedTools.verifyResourceContains(path, status, assertions)
    },
    InputKey[Unit]("checkLogPipelineStages") := {
      val transformCount = BufferLogger.messages.count(_ == "Running transform")
      if (transformCount != 1) {
        sys.error(
          s"""sbt web pipeline stage "transform" found $transformCount time(s) in logs, should run exactly once however.
             |Output:
             |    ${BufferLogger.messages.reverse.mkString("\n    ")}""".stripMargin
        )
      }
      val csCount = BufferLogger.messages.count(_ == "CoffeeScript compiling on 1 source(s)")
      if (csCount != 1) {
        sys.error(
          s"""sbt web pipeline stage "coffeescript" found $csCount time(s) in logs, should run exactly once however.
             |Output:
             |    ${BufferLogger.messages.reverse.mkString("\n    ")}""".stripMargin
        )
      }
    },
    InputKey[Unit]("resetBufferLoggerHelper") := {
      BufferLogger.resetMessages()
    },
    InputKey[Unit]("countFiles") := {
      val args            = Def.spaceDelimited("<filename> <expectedCount> [subDirPath]").parsed
      val originalBaseDir = (ThisBuild / baseDirectory).value

      if (args.length < 2 || args.length > 3) {
        sys.error("Usage: countFiles <filename> <expectedCount> [subDirPath]")
      } else {
        val filename      = args(0)
        val expectedCount = args(1).toInt
        val baseDir =
          if (args.length == 3) originalBaseDir.toPath.resolve(args(2)).normalize() else originalBaseDir.toPath

        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
          sys.error(s"The path '$baseDir' is not a valid directory.")
        }

        val matcher = FileSystems.getDefault.getPathMatcher("glob:**/" + filename)

        val fileCount = Files
          .walk(baseDir)
          .filter(Files.isRegularFile(_))
          .filter(matcher.matches(_))
          .count()

        if (fileCount != expectedCount) {
          sys.error(s"Expected $expectedCount files named $filename, but found $fileCount.")
        } else {
          println(s"Found $fileCount files named $filename, as expected.")
        }
      }
    },
    InputKey[Unit]("checkUnzipListing") := {
      val args    = Def.spaceDelimited("<zipfile> <difffile>").parsed
      val baseDir = (ThisBuild / baseDirectory).value

      if (args.length != 2) {
        sys.error("Usage: checkUnzipListing <zipfile> <difffile>")
      } else {
        val zipfile          = args(0)
        val vanilla_difffile = args(1)

        val unzipcmd    = s"unzip -l $zipfile" // We assume the system has unzip installed...
        val unzipOutput = Process(unzipcmd, baseDir).!!

        val difffile = if (vanilla_difffile.endsWith(".jar.txt")) {
          vanilla_difffile
        } else {
          vanilla_difffile + (if (scalaBinaryVersion.value == "3") {
                                ".scala3.jar.txt"
                              } else {
                                ".scala2.jar.txt"
                              })
        }

        val difffile_content = IO.readLines(new File(difffile)).mkString("\n") + "\n"

        println(s"\nComparing unzip listing of file $zipfile with contents of $difffile")
        println(s"### $zipfile")
        print(unzipOutput)
        println(s"### $difffile")
        print(difffile_content)
        println(s"###")

        if (unzipOutput != difffile_content) {
          sys.error(s"Unzip listing ('$unzipcmd') does not match expected content!")
        } else {
          println(s"Listing of $zipfile as expected.")
        }
        println()
      }
    },
  )
