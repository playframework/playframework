package play.core.coffeescript

import java.io._
import sbt.PlayExceptions.AssetCompilationException

object CoffeescriptCompiler {

  import org.mozilla.javascript._
  import org.mozilla.javascript.tools.shell._

  import scala.collection.JavaConverters._

  import scalax.file._

  private lazy val compiler: (File, Boolean, Boolean) => (String, Option[String]) = {

    (source: File, bare: Boolean, map: Boolean) =>
      {
        withJsContext { (ctx: Context, scope: Scriptable) =>
          val wrappedCoffeescriptCompiler = Context.javaToJS(this, scope)
          ScriptableObject.putProperty(scope, "CoffeescriptCompiler", wrappedCoffeescriptCompiler)

          ctx.evaluateReader(scope, new InputStreamReader(
            this.getClass.getClassLoader.getResource("coffee-script.js").openConnection().getInputStream()),
            "coffee-script.js",
            1, null)

          val coffee = scope.get("CoffeeScript", scope).asInstanceOf[NativeObject]
          val compilerFunction = coffee.get("compile", scope).asInstanceOf[Function]
          val coffeeCode = Path(source).string.replace("\r", "")
          val options = ctx.newObject(scope)
          def compile() = compilerFunction.call(ctx, scope, scope, Array(coffeeCode, options))
          options.put("filename", options, source.getName)
          options.put("bare", options, bare)
          options.put("sourceMap", options, map)

          if (map) {
            options.put("sourceFiles", options, ctx.newArray(scope, List(source.getName).asJava.toArray))

            val nativeObject = compile().asInstanceOf[NativeObject]
            val props = nativeObject.entrySet.asScala.foldLeft(Map[String, String]()) { (map, entry) =>
              map + (entry.getKey.toString -> entry.getValue.toString)
            }

            (props("js"), Some(props("v3SourceMap")))
          } else
            (compile().asInstanceOf[String], None)
        }
      }

  }

  /**
   * wrap function call into rhino context attached to current thread
   * and ensure that it exits right after
   * @param f their name
   */
  def withJsContext(f: (Context, Scriptable) => (String, Option[String])): (String, Option[String]) = {
    val ctx = Context.enter
    ctx.setOptimizationLevel(-1)
    val global = new Global
    global.init(ctx)
    val scope = ctx.initStandardObjects(global)

    try {
      f(ctx, scope)
    } catch {
      case e: Exception => throw e;
    } finally {
      Context.exit
    }
  }

  private def executeNativeCompiler(in: String, source: File): String = {
    import scala.sys.process._
    val qb = Process(in)
    var out = List[String]()
    var err = List[String]()
    val exit = qb ! ProcessLogger((s) => out ::= s, (s) => err ::= s)
    if (exit != 0) {
      val eRegex = """.*Parse error on line (\d+):.*""".r
      val errReverse = err.reverse
      val r = eRegex.unapplySeq(errReverse.mkString("")).map(_.head.toInt)
      throw AssetCompilationException(Some(source), errReverse.mkString("\n"), r, None)
    }
    out.reverse.mkString("\n")
  }

  def compile(source: File, options: Seq[String]): (String, Option[String]) = {
    val nativeCompiler = {
      val index = options.indexOf("native")
      // if "native" is defined, it must be followed by the command
      if (index > 0 && index < options.size)
        Some(options(index + 1))
      else
        None
    }
    try {
      nativeCompiler.map { command =>
        (play.core.jscompile.JavascriptCompiler.executeNativeCompiler(command + " " + source.getAbsolutePath, source), None)
      } getOrElse {
        compiler(source, options.contains("bare"), options.contains("map"))
      }
    } catch {
      case e: JavaScriptException => {
        val log = new sbt.JvmLogger()

        // Starting from CoffeeScript 1.6.2, exceptions' locations are passed within the exception.location property
        val error = e.getValue.asInstanceOf[Scriptable]
        val message = ScriptableObject.getProperty(error, "message").asInstanceOf[String]
        val location = ScriptableObject.getProperty(error, "location").asInstanceOf[NativeObject]
        val locationProps = "first_line" :: "last_line" :: "first_column" :: "last_column" :: Nil

        val List(firstLine, lastLine, firstCol, lastCol) = locationProps.map { prop =>
          ScriptableObject.getProperty(location, prop).asInstanceOf[Any] match {
            case i: Int => i
            case d: Double => d.toInt
            case v =>
              log.error("CoffeeScript compilation error: unmatched type (" + v.getClass + ") for location." + prop + " (" + v + ")")
              0
          }
        }

        throw AssetCompilationException(Some(source), message, Some(firstLine + 1), None)
      }
    }
  }

}
