// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

import java.nio.file._
import java.util.concurrent.atomic.AtomicInteger

import scala.sys.process.Process

import com.typesafe.sbt.coffeescript.SbtCoffeeScript.autoImport.CoffeeScriptKeys.coffeescript
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping

@transient val transform = taskKey[Pipeline.Stage]("transformer")

@transient val transformRuns    = new AtomicInteger
@transient val coffeeScriptRuns = new AtomicInteger

lazy val root = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(
    ScriptedTools.stableUniversalStagingDirectory,
    name          := "assets-pipeline",
    scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties(),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    WebKeys.webTarget            := baseDirectory.value / "target" / "web",
    cleanFiles += WebKeys.webTarget.value,
    libraryDependencies += guice,
    // can't use test directory since scripted calls its script "test"
    Test / sourceDirectory := baseDirectory.value / "tests",
    Test / scalaSource     := baseDirectory.value / "tests",
    transform              := play.sbt.PluginCompat.uncached { (mappings: Seq[PathMapping]) =>
      transformRuns.incrementAndGet()
      mappings
    },
    Assets / coffeescript := play.sbt.PluginCompat.uncached {
      val result = (Assets / coffeescript).value
      coffeeScriptRuns.incrementAndGet()
      result
    },
    Assets / pipelineStages                  := Seq(transform),
    InputKey[Unit]("verifyResourceContains") := {
      val args       = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path       = args.head
      val status     = args.tail.head.toInt
      val assertions = args.tail.tail
      ScriptedTools.verifyResourceContains(path, status, assertions)
    },
    InputKey[Unit]("checkLogPipelineStages") := {
      val transformCount = transformRuns.get()
      if (transformCount != 1) {
        sys.error(
          s"""sbt web pipeline stage "transform" ran $transformCount time(s), expected exactly once"""
        )
      }
      val csCount = coffeeScriptRuns.get()
      if (csCount != 1) {
        sys.error(
          s"""sbt web pipeline stage "coffeescript" ran $csCount time(s), expected exactly once"""
        )
      }
    },
    InputKey[Unit]("resetBufferLoggerHelper") := {
      transformRuns.set(0)
      coffeeScriptRuns.set(0)
    },
    InputKey[Unit]("countFiles") := {
      val args            = Def.spaceDelimited("<filename> <expectedCount> [subDirPath]").parsed
      val originalBaseDir = (ThisBuild / baseDirectory).value

      if (args.length < 2 || args.length > 3) {
        sys.error("Usage: countFiles <filename> <expectedCount> [subDirPath]")
      } else {
        val filename      = args(0)
        val expectedCount = args(1).toInt
        val baseDir       =
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

        // Compiled class sizes vary between Scala compiler releases, and sbt-web
        // adds an Sbt-Web-Module manifest attribute when products are JARs. The
        // archive entry names and timestamps are what this test needs to compare.
        // "      303  2010-01-01 00:00   META-INF/MANIFEST.MF"
        // becomes
        // "<size>  2010-01-01 00:00   META-INF/MANIFEST.MF".
        def normalizeArchiveSizes(listing: String) =
          listing.linesIterator
            .map { line =>
              line.replaceFirst("^\\s*\\d+(?=\\s)", "<size>")
            }
            .mkString("\n") + "\n"

        println(s"\nComparing unzip listing of file $zipfile with contents of $difffile")
        println(s"### $zipfile")
        print(unzipOutput)
        println(s"### $difffile")
        print(difffile_content)
        println(s"###")

        if (normalizeArchiveSizes(unzipOutput) != normalizeArchiveSizes(difffile_content)) {
          sys.error(s"Unzip listing ('$unzipcmd') does not match expected content!")
        } else {
          println(s"Listing of $zipfile as expected.")
        }
        println()
      }
    },
  )
