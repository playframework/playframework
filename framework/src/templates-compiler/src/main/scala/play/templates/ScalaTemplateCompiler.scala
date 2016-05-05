//Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
import java.io.File
import java.security.CodeSource
import java.security.MessageDigest

import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.annotation.migration
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.io.Codec
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.reflect.internal.Flags
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.internal.util.Position
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.Global
import scala.tools.nsc.interactive.Response
import scala.tools.nsc.reporters.ConsoleReporter
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.CharSequenceReader
import scala.util.parsing.input.NoPosition
import scala.util.parsing.input.OffsetPosition
import scala.util.parsing.input.Positional

import scalax.file.Path

package play.templates {

  import scalax.file._
  import java.io.File
  import scala.annotation.tailrec
  import io.Codec
  import scala.reflect.internal.Flags

  object Hash {

    def apply(bytes: Array[Byte], imports: String): String = {
      import java.security.MessageDigest
      val digest = MessageDigest.getInstance("SHA-1")
      digest.reset()
      digest.update(bytes ++ imports.getBytes)
      digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
    }

  }

  case class TemplateCompilationError(source: File, message: String, line: Int, column: Int) extends RuntimeException(message)

  object MaybeGeneratedSource {

    def unapply(source: File): Option[GeneratedSource] = {
      val generated = GeneratedSource(source)
      if (generated.meta.isDefinedAt("SOURCE")) {
        Some(generated)
      } else {
        None
      }
    }

  }

  sealed trait AbstractGeneratedSource {
    def content: String

    lazy val meta: Map[String, String] = {
      val Meta = """([A-Z]+): (.*)""".r
      val UndefinedMeta = """([A-Z]+):""".r
      Map.empty[String, String] ++ {
        try {
          content.split("-- GENERATED --")(1).trim.split('\n').map { m =>
            m.trim match {
              case Meta(key, value) => (key -> value)
              case UndefinedMeta(key) => (key -> "")
              case _ => ("UNDEFINED", "")
            }
          }.toMap
        } catch {
          case _: Throwable => Map.empty[String, String]
        }
      }
    }

    private def parseMeta(name: String) =
      for (pos <- meta(name).split('|'); c = pos.split("->"))
        yield try {
        Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
      } catch {
        case _: Throwable => (0, 0) // Skip if meta is corrupted
      }

    lazy val matrix: Seq[(Int, Int)] = parseMeta("MATRIX")
    lazy val lines: Seq[(Int, Int)] = parseMeta("LINES")

    private def correctIndex(meta: Seq[(Int, Int)])(generatedIndex: Int): Int =
      meta.indexWhere(p => p._1 > generatedIndex) match {
        case 0 => 0
        case i if i > 0 =>
          val pos = meta(i - 1)
          pos._2 + (generatedIndex - pos._1)
        case _ =>
          val pos = meta.takeRight(1)(0)
          pos._2 + (generatedIndex - pos._1)
      }

    val mapPosition = correctIndex(matrix) _
    val mapLine = correctIndex(lines) _
  }

  case class GeneratedSource(file: File) extends AbstractGeneratedSource {

    def content = Path(file).string

    def needRecompilation(imports: String): Boolean = (!file.exists ||
      // A generated source already exist but
      source.isDefined && ((source.get.lastModified > file.lastModified) || // the source has been modified
        (meta("HASH") != Hash(Path(source.get).byteArray, imports))) // or the hash don't match
    )

    def toSourcePosition(marker: Int): (Int, Int) = {
      try {
        val targetMarker = mapPosition(marker)
        val line = Path(source.get).string.substring(0, targetMarker).split('\n').size
        (line, targetMarker)
      } catch {
        case _: Throwable => (0, 0)
      }
    }

    def source: Option[File] = {
      val s = new File(meta("SOURCE"))
      if (s == null || !s.exists) {
        None
      } else {
        Some(s)
      }
    }

    def sync() {
      if (file.exists && !source.isDefined) {
        file.delete()
      }
    }

  }

  case class GeneratedSourceVirtual(path: String) extends AbstractGeneratedSource {
    var _content = ""
    def setContent(newContent: String) {
      this._content = newContent
    }
    def content = _content
  }

  object ScalaTemplateCompiler extends TemplateParser with TemplateElements with TemplateGenerator {

    import scala.util.parsing.input.CharSequenceReader
    import scala.util.parsing.combinator.JavaTokenParsers

    def compile(source: File, sourceDirectory: File, generatedDirectory: File, formatterType: String, additionalImports: String = "") = {
      val resultType = formatterType + ".Appendable"
      val (templateName, generatedSource) = generatedFile(source, sourceDirectory, generatedDirectory)
      if (generatedSource.needRecompilation(additionalImports)) {
        val generated = parseAndGenerateCode(templateName, Path(source).byteArray, source.getAbsolutePath, resultType, formatterType, additionalImports)

        Path(generatedSource.file).write(generated.toString)

        Some(generatedSource.file)
      } else {
        None
      }
    }

    def compileVirtual(content: String, source: File, sourceDirectory: File, resultType: String, formatterType: String, additionalImports: String = "") = {
      val (templateName, generatedSource) = generatedFileVirtual(source, sourceDirectory)
      val generated = parseAndGenerateCode(templateName, content.getBytes(Codec.UTF8.charSet), source.getAbsolutePath, resultType, formatterType, additionalImports)
      generatedSource.setContent(generated)
      generatedSource
    }

    def parseAndGenerateCode(templateName: Array[String], content: Array[Byte], absolutePath: String, resultType: String, formatterType: String, additionalImports: String) = {
      templateParser.parser(new CharSequenceReader(new String(content, Codec.UTF8.charSet))) match {
        case templateParser.Success(parsed: Template, rest) if rest.atEnd => {
          generateFinalTemplate(absolutePath,
            content,
            templateName.dropRight(1).mkString("."),
            templateName.takeRight(1).mkString,
            parsed,
            resultType,
            formatterType,
            additionalImports)
        }
        case templateParser.Success(_, rest) => {
          throw new TemplateCompilationError(new File(absolutePath), "Not parsed?", rest.pos.line, rest.pos.column)
        }
        case templateParser.NoSuccess(message, input) => {
          throw new TemplateCompilationError(new File(absolutePath), message, input.pos.line, input.pos.column)
        }
      }
    }

    def generatedFile(template: File, sourceDirectory: File, generatedDirectory: File) = {
      val templateName = source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head).split('.')
      templateName -> GeneratedSource(new File(generatedDirectory, templateName.mkString("/") + ".template.scala"))
    }

    def generatedFileVirtual(template: File, sourceDirectory: File) = {
      val templateName = source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head).split('.')
      templateName -> GeneratedSourceVirtual(templateName.mkString("/") + ".template.scala")
    }

    @tailrec
    def source2TemplateName(f: File, sourceDirectory: File, ext: String, suffix: String = "", topDirectory: String = "views", setExt: Boolean = true): String = {
      val Name = """([a-zA-Z0-9_]+)[.]scala[.]([a-z]+)""".r
      (f, f.getName) match {
        case (f, _) if f == sourceDirectory => {
          if (setExt) {
            val parts = suffix.split('.')
            Option(parts.dropRight(1).mkString(".")).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + ext + "." + parts.takeRight(1).mkString
          } else suffix
        }
        case (f, name) if name == topDirectory => source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + ext + "." + suffix, topDirectory, false)
        case (f, Name(name, _)) if f.isFile => source2TemplateName(f.getParentFile, sourceDirectory, ext, name, topDirectory, setExt)
        case (f, name) if !f.isFile => source2TemplateName(f.getParentFile, sourceDirectory, ext, name + "." + suffix, topDirectory, setExt)
        case (f, name) => throw TemplateCompilationError(f, "Invalid template name [" + name + "]", 0, 0)
      }
    }

    val templateParser = new TemplateParser

  }

  /* ------- */

  import scala.util.parsing.input.{ Position, OffsetPosition, NoPosition }

  case class Source(code: String, pos: Position = NoPosition)

  object Source {

    import scala.collection.mutable.ListBuffer

    def finalSource(absolutePath: String, contents: Array[Byte], generatedTokens: Seq[Any], hash: String): String = {
      val scalaCode = new StringBuilder
      val positions = ListBuffer.empty[(Int, Int)]
      val lines = ListBuffer.empty[(Int, Int)]
      serialize(generatedTokens, scalaCode, positions, lines)
      scalaCode + """
                /*
                    -- GENERATED --
                    DATE: """ + new java.util.Date + """
                    SOURCE: """ + absolutePath.replace(File.separator, "/") + """
                    HASH: """ + hash + """
                    MATRIX: """ + positions.map { pos =>
        pos._1 + "->" + pos._2
      }.mkString("|") + """
                    LINES: """ + lines.map { line =>
        line._1 + "->" + line._2
      }.mkString("|") + """
                    -- GENERATED --
                */
            """
    }

    private def serialize(parts: Seq[Any], source: StringBuilder, positions: ListBuffer[(Int, Int)], lines: ListBuffer[(Int, Int)]) {
      parts.foreach {
        case s: String => source.append(s)
        case Source(code, pos @ OffsetPosition(_, offset)) => {
          source.append("/*" + pos + "*/")
          positions += (source.length -> offset)
          lines += (source.toString.split('\n').size -> pos.line)
          source.append(code)
        }
        case Source(code, NoPosition) => source.append(code)
        case s: Seq[any] => serialize(s, source, positions, lines)
      }
    }

  }

}
