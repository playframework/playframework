/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.specs2.execute.Result
import org.specs2.main.CommandLine
import org.specs2.mutable.Specification
import play.routes.compiler.RoutesCompiler.RoutesCompilerTask

/**
 * Compares the sources generated for `snapshot.routes` with the expected sources in `generated-sources`. Trailing
 * whitespace is not recorded, so that editors and whitespace checks leave the expected sources alone.
 *
 * After an intended change to the generated sources, update the expected sources with:
 * {{{
 * sbt "Play-Routes-Compiler/testOnly play.routes.compiler.GeneratedSourcesSpec -- updateGeneratedSources"
 * }}}
 */
class GeneratedSourcesSpec(commandLine: CommandLine) extends Specification {
  sequential

  private val Suffix = ".txt"

  // Scala 2.12 iterates hash maps in another order, so the routes compiler for sbt 1 orders some classes and names
  // differently. That output is deterministic as well, but only the order of the newer Scala versions is recorded here.
  private val isScala212 = scala.util.Properties.versionNumberString.startsWith("2.12.")

  "the routes compiler" should {
    "generate the expected Scala sources" in checkGeneratedSources(Language.SCALA, "scala")
    "generate the expected Java sources" in checkGeneratedSources(Language.JAVA, "java")
  }

  private def checkGeneratedSources(lang: Language, dirName: String): Result =
    if (isScala212) skipped("The expected sources are recorded with Scala 2.13 and 3")
    else checkGeneratedSourcesIn(lang, dirName)

  private def checkGeneratedSourcesIn(lang: Language, dirName: String): Result = withTempDir { tmp =>
    val generated = generate(lang, tmp)
    // Read from the project directory, because the test resources may be packaged in a jar
    val resources = new File("src/test/resources")
    if (!resources.isDirectory) {
      throw new IllegalStateException(
        s"Run the tests from the project directory, not from ${new File(".").getCanonicalPath}"
      )
    }
    val expectedDir = new File(resources, s"generated-sources/$dirName")
    if (commandLine.contains("updateGeneratedSources")) {
      delete(expectedDir)
      generated.foreach {
        case (name, content) =>
          val file = new File(expectedDir, name + Suffix)
          file.getParentFile.mkdirs()
          Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8))
      }
    }
    val expected = listFiles(expectedDir).collect {
      case file if file.getName.endsWith(Suffix) =>
        relativePath(expectedDir, file).stripSuffix(Suffix) -> read(file)
    }.toMap

    val differences = (generated.keySet ++ expected.keySet).toSeq.sorted.flatMap { name =>
      (generated.get(name), expected.get(name)) match {
        case (Some(actual), Some(wanted)) if actual == wanted => None
        case (Some(actual), Some(wanted))                     =>
          val line = actual.linesIterator.zipAll(wanted.linesIterator, "", "").indexWhere(p => p._1 != p._2) + 1
          Some(s"$name differs from line $line")
        case (Some(_), None) => Some(s"$name is not expected")
        case (None, _)       => Some(s"$name is missing")
      }
    }
    (differences.aka(
      s"differences to generated-sources/$dirName (see the GeneratedSourcesSpec scaladoc)"
    ) must beEmpty).toResult
  }

  private def generate(lang: Language, tmp: File): Map[String, String] = {
    val routes = new File(tmp, "snapshot.routes")
    val stream = getClass.getClassLoader.getResourceAsStream("snapshot.routes")
    try Files.copy(stream, routes.toPath)
    finally stream.close()

    val out = new File(tmp, "out")
    RoutesCompiler.compile(
      RoutesCompilerTask(routes, Seq.empty, true, true, true, false, lang),
      InjectedRoutesGenerator,
      out
    ) match {
      case Right(files) =>
        files
          .map(file =>
            relativePath(out, file) -> read(file)
              .replaceAll("(?m)^// @SOURCE:.*$", "// @SOURCE:snapshot.routes")
              .replaceAll("(?m)[ \t]+$", "")
          )
          .toMap
      case Left(errors) => throw new IllegalStateException(errors.mkString("\n"))
    }
  }

  private def read(file: File): String = new String(Files.readAllBytes(file.toPath), StandardCharsets.UTF_8)

  private def relativePath(dir: File, file: File): String =
    dir.toPath.relativize(file.toPath).toString.replace(File.separatorChar, '/')

  private def listFiles(dir: File): Seq[File] =
    Option(dir.listFiles()).toSeq.flatten.flatMap {
      case d if d.isDirectory => listFiles(d)
      case f                  => Seq(f)
    }

  private def delete(file: File): Unit = {
    Option(file.listFiles()).toSeq.flatten.foreach(delete)
    file.delete()
  }

  private def withTempDir[T](block: File => T): T = {
    val tmp = Files.createTempDirectory("GeneratedSourcesSpec").toFile
    try block(tmp)
    finally delete(tmp)
  }
}
