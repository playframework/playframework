package play.core.less

import sbt.PlayExceptions.AssetCompilationException
import java.io._
import play.api._

object LessCompiler {

  import org.mozilla.javascript._
  import org.mozilla.javascript.tools.shell._

  import scala.collection.JavaConverters._

  import scalax.file._

  private def compiler(minify: Boolean) = {
    val ctx = Context.enter
    val global = new Global; global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    val wrappedLessCompiler = Context.javaToJS(this, scope)
    ScriptableObject.putProperty(scope, "LessCompiler", wrappedLessCompiler)

    ctx.evaluateString(scope,
      """
                var timers = [],
                    window = {
                        document: {
                            getElementById: function(id) { 
                                return [];
                            },
                            getElementsByTagName: function(tagName) {
                                return [];
                            }
                        },
                        location: {
                            protocol: 'file:', 
                            hostname: 'localhost', 
                            port: '80'
                        },
                        setInterval: function(fn, time) {
                            var num = timers.length;
                            timers[num] = fn.call(this, null);
                            return num;
                        }
                    },
                    document = window.document,
                    location = window.location,
                    setInterval = window.setInterval;

            """,
      "browser.js",
      1, null)
    ctx.evaluateReader(scope, new InputStreamReader(
      this.getClass.getClassLoader.getResource("less-1.3.0.js").openConnection().getInputStream()),
      "less-1.3.0.js",
      1, null)
    ctx.evaluateString(scope,
      """
                var compile = function(source) {

                    var compiled;
                    var dependencies = [source];

                    window.less.Parser.importer = function(path, paths, fn, env) {
                        if (!/^([a-z]+:)?\//.test(path) && paths.length > 0) {
                            path = paths[0] + path;
                        }
                        var imported = LessCompiler.resolve(source, path);
                        var input = String(LessCompiler.readContent(imported)); 
                        dependencies.push(imported)
                        new(window.less.Parser)({
                            optimization:3,
                            filename:path,
                            paths: [path.replace(/[\w\.-]+$/, '')]
                        }).parse(input, function (e, root) {
                            if(e instanceof Object) {
                                throw e;
                            }
                            fn(e, root, input);
                        });
                    }

                    new(window.less.Parser)({optimization:3, filename:String(source.getCanonicalPath())}).parse(String(LessCompiler.readContent(source)), function (e,root) {
                        if(e instanceof Object) {
                            throw e;
                        }
                        compiled = root.toCSS({compress: """ + (if (minify) "true" else "false") + """})
                    })

                    return {css:compiled, dependencies:dependencies}
                }
            """,
      "compiler.js",
      1, null)
    val compilerFunction = scope.get("compile", scope).asInstanceOf[Function]

    Context.exit

    (source: File) => {
      val result = Context.call(null, compilerFunction, scope, scope, Array(source)).asInstanceOf[Scriptable]
      val css = ScriptableObject.getProperty(result, "css").asInstanceOf[String]
      val dependencies = ScriptableObject.getProperty(result, "dependencies").asInstanceOf[NativeArray]

      css -> (0 until dependencies.getLength.toInt).map(ScriptableObject.getProperty(dependencies, _) match {
        case f: File => f
        case o: NativeJavaObject => o.unwrap.asInstanceOf[File]
      })
    }
  }

  private lazy val debugCompiler = compiler(false)

  private lazy val minCompiler = compiler(true)

  def compile(source: File): (String, Option[String], Seq[File]) = {
    try {
      val debug = debugCompiler(source)
      val min = minCompiler(source)
      (debug._1, Some(min._1), debug._2)
    } catch {
      case e: JavaScriptException => {

        val error = e.getValue.asInstanceOf[Scriptable]
        val filename = ScriptableObject.getProperty(error, "filename").asInstanceOf[String]
        val file = if (filename == source.getAbsolutePath()) source else resolve(source, filename)
        throw AssetCompilationException(Some(file),
          ScriptableObject.getProperty(error, "message").asInstanceOf[String],
          ScriptableObject.getProperty(error, "line").asInstanceOf[Double].intValue,
          ScriptableObject.getProperty(error, "column").asInstanceOf[Double].intValue)
      }
    }
  }

  def readContent(file: File) = Path(file).slurpString.replace("\r", "")
  def resolve(originalSource: File, imported: String) = new File(originalSource.getParentFile, imported)

}
