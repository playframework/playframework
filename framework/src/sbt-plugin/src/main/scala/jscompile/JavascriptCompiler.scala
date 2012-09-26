package play.core.jscompile

import sbt.PlayExceptions.AssetCompilationException
import java.io._
import play.api._
import scala.collection.JavaConverters._
import scalax.file._
import com.google.javascript.rhino.Node
import com.google.javascript.jscomp.ProcessCommonJSModules
import scala.io.Source

object JavascriptCompiler {

  import com.google.javascript.jscomp.{ Compiler, CompilerOptions, JSSourceFile, CompilationLevel }

  /**
   * Compile a JS file with its dependencies
   * @return a triple containing the original source code, the minified source code, the list of dependencies (including the input file)
   */
  def compile(source: File, coptions: Seq[String]): (String, Option[String], Seq[File]) = {
    import scala.util.control.Exception._

    val origin = Path(source).slurpString

    val options = new CompilerOptions()
    options.closurePass = true
    options.setProcessCommonJSModules(true)
    options.setCommonJSModulePathPrefix(source.getParent() + File.separator)
    options.setManageClosureDependencies(Seq(toModuleName(source.getName())).asJava)
    coptions.foreach(_ match {
      case "advancedOptimizations" => CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(options)
      case "checkCaja" => options.setCheckCaja(true)
      case "checkControlStructures" => options.setCheckControlStructures(true)
      case "checkTypes" => options.setCheckTypes(true)
      case "checkSymbols" => options.setCheckSymbols(true)
      case _ => Unit // Unkown option
    })

    val compiler = new Compiler()
    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    val all = allSiblings(source)
    val input = all.map(f => JSSourceFile.fromFile(f)).toArray

    catching(classOf[Exception]).either(compiler.compile(extern, input, options).success) match {
      case Right(true) => (origin, Some(compiler.toSource()), all)
      case Right(false) => {
        val error = compiler.getErrors().head
        val errorFile = all.find(f => f.getAbsolutePath() == error.sourceName)
        throw AssetCompilationException(errorFile, error.description, error.lineNumber, 0)
      }
      case Left(exception) =>
        exception.printStackTrace()
        throw AssetCompilationException(Some(source), "Internal Closure Compiler error (see logs)", 0, 0)
    }
  }

  /**
   * Minify a Javascript string
   */
  def minify(source: String, name: Option[String]): String = {

    val compiler = new Compiler()
    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    val options = new CompilerOptions()

    val input = JSSourceFile.fromCode(name.getOrElse("unknown"), source)

    compiler.compile(extern, input, options).success match {
      case true => compiler.toSource()
      case false => {
        val error = compiler.getErrors().head
        throw AssetCompilationException(None, error.description, error.lineNumber, 0)
      }
    }
  }

  /**
   * Return all Javascript files in the same directory than the input file, or subdirectories
   */
  private def allSiblings(source: File): Seq[File] = allJsFilesIn(source.getParentFile())

  private def allJsFilesIn(dir: File): Seq[File] = {
    import scala.collection.JavaConversions._
    val jsFiles = dir.listFiles(new FileFilter {
      override def accept(f: File) = f.getName().endsWith(".js")
    })
    val directories = dir.listFiles(new FileFilter {
      override def accept(f: File) = f.isDirectory()
    })
    val jsFilesChildren = directories.map(d => allJsFilesIn(d)).flatten
    jsFiles ++ jsFilesChildren
  }

  /**
   * Turns a filename into a JS identifier that is used for moduleNames in
   * rewritten code. Removes leading ./, replaces / with $, removes trailing .js
   * and replaces - with _. All moduleNames get a "module$" prefix.
   */
  private def toModuleName(filename: String) = {
    "module$" + filename.replaceAll("^\\./", "").replaceAll("/", "\\$").replaceAll("\\.js$", "").replaceAll("-", "_");
  }

}
