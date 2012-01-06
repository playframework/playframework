package play.core.jscompile

import sbt.PlayExceptions.AssetCompilationException

import java.io._
import play.api._

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import scalax.file._

import com.google.javascript.rhino.Node

object JavascriptCompiler {

  import com.google.javascript.jscomp.{ Compiler, CompilerOptions, JSSourceFile }

  def compile(source: File): (String, Option[String], Seq[File]) = {

    val compiler = new Compiler()
    val options = new CompilerOptions()

    val tree = SourceTree.build(source)

    val file = Path(source)
    val jsCode = file.slurpString.replace("\r", "")
    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    val input = tree.dependencies.map(file => JSSourceFile.fromCode(file.getName(), SourceTree.requireRe.replaceAllIn(Path(file).slurpString, ""))).toArray

    compiler.compile(extern, input, options).success match {
      case true => (tree.fullSource, Some(compiler.toSource()), tree.dependencies)
      case false => {
        val error = compiler.getErrors().head
        throw AssetCompilationException(Some(source), error.description, error.lineNumber, 0)
      }
    }
  }

  /**
   * Minify a Javascript string
   */
  def minify(source: String, name: Option[String]): String = {

    val compiler = new Compiler()
    val options = new CompilerOptions()

    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    val input = JSSourceFile.fromCode(name.getOrElse("unknown"), source)

    compiler.compile(extern, input, options).success match {
      case true => compiler.toSource()
      case false => {
        val error = compiler.getErrors().head
        throw AssetCompilationException(None, error.description, error.lineNumber, 0)
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
      if (ancestors.contains(root)) throw new AssetCompilationException(Some(node), "Cycle detected in require instruction", lineNo, 0)
      SourceTree.build(node, ancestors + root)
    }).toList)
  }

  val requireRe = """require\(["']([\w\-\.]+)["']\)""".r

  def depsFor(input: File): Iterator[(File, Int)] =
    requireRe.findAllIn(Path(input).slurpString).matchData
      .map(m => (m.before.toString.count(s => (s == '\n')) + 1, m.group(1)))
      .map(pair => { // (lineNo, filename)
        val require = new File(input.getParentFile(), pair._2 + ".js")
        if (!require.canRead || !require.isFile)
          throw new AssetCompilationException(Some(input), "Unable to find file " + pair._2 + ".js", pair._1, 0)
        (require, pair._1)
      })

}

