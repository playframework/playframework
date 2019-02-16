/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files

import scala.collection.JavaConverters._

import scala.io.Codec

/**
 * provides a compiler for routes
 */
object RoutesCompiler {

  private val LineMarker = "\\s*// @LINE:\\s*(\\d+)\\s*".r

  /**
   * A source file that's been generated by the routes compiler
   */
  trait GeneratedSource {

    /**
     * The original source file associated with this generated source file, if known
     */
    def source: Option[File]

    /**
     * Map the generated line to the original source file line, if known
     */
    def mapLine(generatedLine: Int): Option[Int]
  }

  object GeneratedSource {

    def unapply(file: File): Option[GeneratedSource] = {

      val lines: Array[String] = if (file.exists) {
        Files.readAllLines(file.toPath, Charset.forName(implicitly[Codec].name)).asScala.toArray[String]
      } else {
        Array.empty[String]
      }

      if (lines.contains("// @GENERATOR:play-routes-compiler")) {
        Some(new GeneratedSource {
          val source: Option[File] =
            lines.find(_.startsWith("// @SOURCE:")).map(m => new File(m.trim.drop(11)))

          def mapLine(generatedLine: Int): Option[Int] = {
            lines.view.take(generatedLine).reverse.collectFirst {
              case LineMarker(line) => Integer.parseInt(line)
            }
          }
        })
      } else {
        None
      }
    }

  }

  /**
   * A routes compiler task.
   *
   * @param file The routes file to compile.
   * @param additionalImports The additional imports.
   * @param forwardsRouter Whether a forwards router should be generated.
   * @param reverseRouter Whether a reverse router should be generated.
   * @param namespaceReverseRouter Whether the reverse router should be namespaced.
   */
  case class RoutesCompilerTask(
      file: File,
      additionalImports: Seq[String],
      forwardsRouter: Boolean,
      reverseRouter: Boolean,
      namespaceReverseRouter: Boolean
  )

  /**
   * Compile the given routes file
   *
   * @param task The routes compilation task
   * @param generator The routes generator
   * @param generatedDir The directory to place the generated source code in
   * @return Either the list of files that were generated (right) or the routes compilation errors (left)
   */
  def compile(
      task: RoutesCompilerTask,
      generator: RoutesGenerator,
      generatedDir: File
  ): Either[Seq[RoutesCompilationError], Seq[File]] = {

    val namespace = Option(task.file.getName)
      .filter(_.endsWith(".routes"))
      .map(_.dropRight(".routes".size))
      .orElse(Some("router"))

    val routeFile = task.file.getAbsoluteFile

    RoutesFileParser.parse(routeFile).right.map { rules =>
      val generated = generator.generate(task, namespace, rules)
      generated.map {
        case (filename, content) =>
          val file = new File(generatedDir, filename)
          if (!file.exists()) {
            file.getParentFile.mkdirs()
            file.createNewFile()
          }
          Files.write(file.toPath, content.getBytes(implicitly[Codec].name))
          file
      }
    }
  }
}
