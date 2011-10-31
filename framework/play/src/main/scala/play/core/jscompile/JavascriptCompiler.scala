package play.core.jscompile

import java.io._
import play.api._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import scalax.file._

import com.google.javascript.rhino.Node

object JavascriptCompiler {

  import com.google.javascript.jscomp.{ Compiler, CompilerOptions, JSSourceFile }

  def compile(source: File, minify: Boolean): (String, String, Seq[File]) = {

    val compiler = new Compiler()
    val options = new CompilerOptions()

    val tree = SourceTree.build(source)
    println("Dependencies (" + source.getName() + "): ")

    val file = Path(source)
    val jsCode = file.slurpString.replace("\r", "")
    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    val input = tree.dependencies.map(file => JSSourceFile.fromCode(file.getName(), SourceTree.requireRe.replaceAllIn(Path(file).slurpString, ""))).toArray

    compiler.compile(extern, input, options).success match {
      case true => (tree.fullSource, compiler.toSource(), List(source))
      case false => {
        val error = compiler.getErrors().first
        throw CompilationException(error.description, source, error.lineNumber, 0)
      }
    }
  }

}

case class SourceTree(node: File, ancestors: Set[File] = Set(), children: List[SourceTree] = List()) {
  override def toString = print()
  def print(indent: String = ""): String = (indent + node.getName() + "\n" + children.mkString("\n"))
  private lazy val flatDependencies: List[File] = node +: children.flatMap(_.flatDependencies)
  lazy val dependencies: List[File] = flatDependencies.reverse.distinct
  lazy val fullSource = dependencies.map(Path(_).slurpString).mkString("\n")
}

object SourceTree {

  def build(root: File, ancestors: Set[File] = Set()): SourceTree = {
    SourceTree(root, ancestors, depsFor(root).map(pair => {
      val node = pair._1
      val lineNo = pair._2
      // Check for cycles
      if (ancestors.contains(root)) throw new CompilationException("Cycle detected in require instruction", node, lineNo, 0)
      SourceTree.build(node, ancestors + root)
    }).toList)
  }

  val requireRe = """require\("([^"]+)"\)""".r

  def depsFor(input: File): Iterator[(File, Int)] =
    requireRe.findAllIn(Path(input).slurpString).matchData
      .map(m => (m.before.toString.count(s => (s == '\n')) + 1, m.group(1)))
      .map(pair => { // (lineNo, filename)
        val require = new File(input.getParentFile(), pair._2 + ".js")
        if (!require.canRead || !require.isFile)
          throw new CompilationException("Unable to find file " + pair._2 + ".js", input, pair._1, 0)
        (require, pair._1)
      })

}

case class CompilationException(message: String, jsFile: File, atLine: Int, atColumn: Int) extends PlayException(
  "Compilation error", message) with PlayException.ExceptionSource {
  def line = Some(atLine)
  def position = Some(atColumn)
  def input = Some(scalax.file.Path(jsFile))
  def sourceName = Some(jsFile.getAbsolutePath)
}

