package play.core.less

import play.PlayExceptions.AssetCompilationException
import java.io._
import play.api._

object LessCompiler {

  val lessScript = "less-1.4.2.js"

  import org.mozilla.javascript._
  import org.mozilla.javascript.tools.shell._

  import scala.collection.JavaConverters._

  import scalax.file._

  /**
   * Create a compiler.  Returns a function that can be used to compile less files.
   */
  private def createCompiler(minify: Boolean): File => (String, Seq[File]) = {
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
      this.getClass.getClassLoader.getResource(lessScript).openConnection().getInputStream()),
      lessScript,
      1, null)
    ctx.evaluateString(scope,
      """
                var compile = function(source) {

                    var compiled;
                    // Import tree context
                    var context = [source];
                    var dependencies = [source];

                    window.less.Parser.importer = function(path, paths, fn, env) {

                        var imported = LessCompiler.resolve(context[context.length - 1], path);
                        var importedName = String(imported.getCanonicalPath());
                        try {
                          var input = String(LessCompiler.readContent(imported));
                        } catch (e) {
                          return fn({ type: "File", message: "File not found: " + importedName });
                        }

                        // Store it in the contents, for error reporting
                        env.contents[importedName] = input;

                        context.push(imported);
                        dependencies.push(imported);

                        new(window.less.Parser)({
                            optimization:3,
                            filename:importedName,
                            contents:env.contents,
                            dumpLineNumbers:window.less.dumpLineNumbers
                        }).parse(input, function (e, root) {
                            fn(e, root, input);

                            context.pop();
                        });
                    }

                    new(window.less.Parser)({optimization:3, filename:String(source.getCanonicalPath())}).parse(String(LessCompiler.readContent(source)), function (e,root) {
                        if (e) {
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

    // Since SBT executes things in parallel by default, and since the LESS compiler is stateful requiring tracking
    // context, we need to ensure that access to the compiler is synchronized.
    val mutex = new Object()

    (source: File) => {
      mutex.synchronized {
        val result = Context.call(null, compilerFunction, scope, scope, Array(source)).asInstanceOf[Scriptable]
        val css = ScriptableObject.getProperty(result, "css").toString
        val dependencies = ScriptableObject.getProperty(result, "dependencies").asInstanceOf[NativeArray]

        css -> (0 until dependencies.getLength.toInt).map(ScriptableObject.getProperty(dependencies, _) match {
          case f: File => f.getCanonicalFile
          case o: NativeJavaObject => o.unwrap.asInstanceOf[File].getCanonicalFile
        })
      }
    }
  }

  private lazy val debugCompiler = createCompiler(false)

  private lazy val minCompiler = createCompiler(true)

  def compile(source: File): (String, Option[String], Seq[File]) = {
    try {
      val debug = debugCompiler(source)
      val min = minCompiler(source)
      (debug._1, Some(min._1), debug._2)
    } catch {
      case e: JavaScriptException => {

        val error = e.getValue.asInstanceOf[Scriptable]
        val filename = ScriptableObject.getProperty(error, "filename").toString
        val file = new File(filename)
        throw AssetCompilationException(Some(file),
          ScriptableObject.getProperty(error, "message").toString,
          Some(ScriptableObject.getProperty(error, "line").asInstanceOf[Double].intValue),
          Some(ScriptableObject.getProperty(error, "column").asInstanceOf[Double].intValue))
      }
    }
  }

  def readContent(file: File) = Path(file).string.replace("\r", "")
  def resolve(originalSource: File, imported: String) = new File(originalSource.getParentFile, imported)

}
