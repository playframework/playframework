package play.core.jscompile

import sbt.PlayExceptions.AssetCompilationException

import java.io._
import play.api._

import scala.collection.JavaConverters._

import scalax.file._

import com.google.javascript.rhino.Node

object JavascriptCompiler {

  import com.google.javascript.jscomp.{ Compiler, CompilerOptions, JSSourceFile }

  /**
   * Compile a JS file with its dependencies
   * @return a triple containing the unminifed source code, the minified source code, the list of dependencies (including the input file)
   */
  def compile(source: File): (String, Option[String], Seq[File]) = {

    val compiler = new Compiler()
    val options = new CompilerOptions()

    val tree = SourceTree.build(source)

    val file = Path(source)
    val jsCode = file.slurpString.replace("\r", "")
    val extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}")
    // Excluding the current file
    val deps = tree.dependencies.filterNot(_.file == source)
    val input = deps.map(file => JSSourceFile.fromCode(file.key, includeSource(file)))

    compiler.compile(extern, (headerSource +: input :+ JSSourceFile.fromCode(source.getName(), jsCode)).toArray, options).success match {
      case true => (tree.fullSource, Some(compiler.toSource()), tree.dependencies.map(_.file))
      case false => {
        val error = compiler.getErrors().head
        throw AssetCompilationException(tree.findSource(error.sourceName).map(_.file), error.description, error.lineNumber, 0)
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

  val requireSource = """
function require(p){
    var path = require.resolve(p)
      , mod = require.modules[path];
    if (!mod) throw new Error('failed to require "' + p + '"');
    if (!mod.exports) {
      mod.exports = {};
      mod.call(mod.exports, mod, mod.exports, require.relative(path));
    }
    return mod.exports;
  }

require.modules = {};

require.resolve = function (path){
    var orig = path, reg = path + '.js', index = path + '/index.js';
    return require.modules[reg] && reg
      || require.modules[index] && index
      || orig;
  };

require.register = function (path, fn){
    require.modules[path] = fn;
  };

require.relative = function (parent) {
    return function(p){
      if ('.' != p[0]) return require(p);
      var path = parent.split('/')
        , segs = p.split('/');
      path.pop();
      for (var i = 0; i < segs.length; i++) {
        var seg = segs[i];
        if ('..' == seg) path.pop();
        else if ('.' != seg) path.push(seg);
      }
      return require(path.join('/'));
    };
  };

"""

  lazy val headerSource = JSSourceFile.fromCode("require", requireSource)

  def includeSource(res: Resource) =
    "require.register(\"" + res.key + "\", function(module, exports, require){ " + Path(res.file).slurpString + "});\n"

}

case class Resource(key: String, file: File)

/**
 * This is used to resolve dependencies between source files
 */
case class SourceTree(node: Resource, ancestors: Set[File] = Set(), children: List[SourceTree] = List()) {

  override def toString = print()

  def print(indent: String = ""): String = (indent + node.key + "\n" + children.mkString("\n"))

  private lazy val flatDependencies: List[Resource] = node +: children.flatMap(_.flatDependencies)

  lazy val dependencies: List[Resource] = flatDependencies.reverse.distinct

  def findSource(key: String): Option[Resource] = (node, children) match {
    case (Resource(k, _), _) if k == key => Some(node)
    case (_, Nil) => None
    case (_, children) => children.find(_.findSource(key).isDefined).flatMap(_.findSource(key))
    case _ => None
  }

  def fullSource = if (children.size == 0)
    Path(node.file).slurpString.replace("\r", "")
  else
    JavascriptCompiler.requireSource + dependencies.dropRight(1).map(res => JavascriptCompiler.includeSource(res)).mkString("\n\n") + Path(node.file).slurpString.replace("\r", "")

}

object SourceTree {

  def build(root: File): SourceTree = build(Resource(root.getName(), root))

  def build(root: Resource, ancestors: Set[File] = Set()): SourceTree = {
    SourceTree(root, ancestors, depsFor(root.file).map(pair => {
      val key = pair._1
      val node = pair._2
      val lineNo = pair._3
      // Check for cycles
      if (ancestors.contains(node))
        throw new AssetCompilationException(Some(node), "Cycle detected in require instruction", lineNo, 0)
      SourceTree.build(Resource(key, node), ancestors + root.file)
    }).toList)
  }

  val requireRe = """require\(["']([\w\-\./_]+)["']\)""".r

  // Iterator of key, file and line number where we found it
  def depsFor(input: File): Iterator[(String, File, Int)] =
    requireRe.findAllIn(Path(input).slurpString).matchData
      .map(m => (m.before.toString.count(s => (s == '\n')) + 1, m.group(1)))
      .map(pair => { // (lineNo, filename)
        val require = new File(input.getParentFile(), pair._2 + ".js")
        if (!require.canRead || !require.isFile)
          throw new AssetCompilationException(Some(input), "Unable to find file " + pair._2 + ".js", pair._1, 0)
        (pair._2, require, pair._1)
      })

}

