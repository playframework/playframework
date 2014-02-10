package play.core.jscompile

import play.PlayExceptions.AssetCompilationException
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
   * @param source
   * @param simpleCompilerOptions user supplied simple command line parameters
   * @param fullCompilerOptions user supplied full blown CompilerOptions instance
   */
  def compile(source: File, simpleCompilerOptions: Seq[String], fullCompilerOptions: Option[CompilerOptions]): (String, Option[String], Seq[File]) = {
    import scala.util.control.Exception._

    val requireJsMode = simpleCompilerOptions.contains("rjs")
    val commonJsMode = simpleCompilerOptions.contains("commonJs") && !requireJsMode

    val origin = Path(source).string

    val options = fullCompilerOptions.getOrElse {
      val defaultOptions = new CompilerOptions()
      defaultOptions.closurePass = true

      if (commonJsMode) {
        defaultOptions.setProcessCommonJSModules(true)
        // The compiler always expects forward slashes even on Windows.
        defaultOptions.setCommonJSModulePathPrefix((source.getParent() + File.separator).replaceAll("\\\\", "/"))
        defaultOptions.setManageClosureDependencies(Seq(toModuleName(source.getName())).asJava)
      }

      simpleCompilerOptions.foreach(_ match {
        case "advancedOptimizations" => CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(defaultOptions)
        case "checkCaja" => defaultOptions.setCheckCaja(true)
        case "checkControlStructures" => defaultOptions.setCheckControlStructures(true)
        case "checkTypes" => defaultOptions.setCheckTypes(true)
        case "checkSymbols" => defaultOptions.setCheckSymbols(true)
        case "ecmascript5" => defaultOptions.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5)
        case _ => Unit // Unknown option
      })
      defaultOptions
    }

    val compiler = new Compiler()
    lazy val all = allSiblings(source)
    // In commonJsMode, we use all JavaScript sources in the same directory for some reason.
    // Otherwise, we only look at the current file.
    val input = if (commonJsMode) all.map(f => JSSourceFile.fromFile(f)).toArray else Array(JSSourceFile.fromFile(source))

    catching(classOf[Exception]).either(compiler.compile(Array[JSSourceFile](), input, options).success) match {
      case Right(true) => (origin, { if (!requireJsMode) Some(compiler.toSource()) else None }, Nil)
      case Right(false) => {
        val error = compiler.getErrors().head
        val errorFile = all.find(f => f.getAbsolutePath() == error.sourceName)
        throw AssetCompilationException(errorFile, error.description, Some(error.lineNumber), None)
      }
      case Left(exception) =>
        exception.printStackTrace()
        throw AssetCompilationException(Some(source), "Internal Closure Compiler error (see logs)", None, None)
    }
  }

  /**
   * Minify a Javascript string
   */
  def minify(source: String, name: Option[String]): String = {

    val compiler = new Compiler()
    val options = new CompilerOptions()

    val input = Array[JSSourceFile](JSSourceFile.fromCode(name.getOrElse("unknown"), source))

    compiler.compile(Array[JSSourceFile](), input, options).success match {
      case true => compiler.toSource()
      case false => {
        val error = compiler.getErrors().head
        throw AssetCompilationException(None, error.description, Some(error.lineNumber), None)
      }
    }
  }

  case class CompilationException(message: String, jsFile: File, atLine: Option[Int]) extends PlayException.ExceptionSource(
    "JS Compilation error", message) {
    def line = atLine.map(_.asInstanceOf[java.lang.Integer]).orNull
    def position = null
    def input = scalax.file.Path(jsFile).string
    def sourceName = jsFile.getAbsolutePath
  }

  /*
   * execute a native compiler for given command
   */
  def executeNativeCompiler(in: String, source: File): String = {
    import scala.sys.process._
    val qb = Process(in)
    var out = List[String]()
    var err = List[String]()
    val exit = qb ! ProcessLogger((s) => out ::= s, (s) => err ::= s)
    if (exit != 0) {
      val eRegex = """.*Parse error on line (\d+):.*""".r
      val errReverse = err.reverse
      val r = eRegex.unapplySeq(errReverse.mkString("")).map(_.head.toInt)
      val error = "error in: " + in + " \n" + errReverse.mkString("\n")

      throw CompilationException(error, source, r)
    }
    out.reverse.mkString("\n")
  }

  def require(source: File): Unit = {
    import org.mozilla.javascript._

    import org.mozilla.javascript.tools.shell._

    import scala.collection.JavaConverters._

    import scalax.file._

    val ctx = Context.enter; ctx.setOptimizationLevel(-1)
    val global = new Global; global.init(ctx)
    val scope = ctx.initStandardObjects(global)
    val writer = new java.io.StringWriter()
    try {
      val defineArguments = """arguments = ['-o', '""" + source.getAbsolutePath.replace(File.separatorChar, '/') + "']"
      ctx.evaluateString(scope, defineArguments, null,
        1, null)
      val r = ctx.evaluateReader(scope, new InputStreamReader(
        this.getClass.getClassLoader.getResource("r.js").openConnection().getInputStream()),
        "r.js", 1, null)
    } finally {
      Context.exit()
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