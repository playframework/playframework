package play.core.jscompile

import java.io._
import play.api._

object JavascriptCompiler {

  import scala.collection.JavaConverters._

  import scalax.file._

  import com.google.javascript.jscomp.{ Compiler, CompilerOptions, JSSourceFile }

  def compile(source: File, minify: Boolean): (String, Seq[File]) = {

    val compiler = new Compiler()
    val options = new CompilerOptions()

    val file = Path(source)
    val jsCode = file.slurpString.replace("\r", "")
    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    val input = JSSourceFile.fromCode(source.getName(), jsCode);
    compiler.compile(extern, input, options).success match {
      case true => (if (minify) compiler.toSource() else jsCode, List() /* TODO: dependencies */ )
      case false => {
        val error = compiler.getErrors().first
        throw CompilationException(error.description, source, error.lineNumber, 0)
      }
    }
  }

  //  def readContent(file: File) = Path(file).slurpString.replace("\r", "")
  //  def resolve(originalSource: File, imported: String) = new File(originalSource.getParentFile, imported)

}

case class CompilationException(message: String, jsFile: File, atLine: Int, atColumn: Int) extends PlayException(
  "Compilation error", message) with PlayException.ExceptionSource {
  def line = Some(atLine)
  def position = Some(atColumn)
  def input = Some(scalax.file.Path(jsFile))
  def sourceName = Some(jsFile.getAbsolutePath)
}

