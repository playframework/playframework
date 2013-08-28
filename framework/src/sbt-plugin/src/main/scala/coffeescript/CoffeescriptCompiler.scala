package play.core.coffeescript

import java.io._
import play.PlayExceptions.AssetCompilationException

object CoffeescriptCompiler {

  import org.mozilla.javascript._
  import org.mozilla.javascript.tools.shell._

  import scalax.file._

  private lazy val compiler = {

    (source: File, bare: Boolean) =>
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
          options.put("bare", options, bare)
          compilerFunction.call(ctx, scope, scope, Array(coffeeCode, options))
        }.toString
      }

  }

  /**
   * wrap function call into rhino context attached to current thread
   * and ensure that it exits right after
   * @param f their name
   */
  def withJsContext(f: (Context, Scriptable) => Any): Any = {
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

  def compile(source: File, options: Seq[String]): String = {
    try {
      if (options.size == 2 && options.headOption.filter(_ == "native").isDefined)
        play.core.jscompile.JavascriptCompiler.executeNativeCompiler(options.last + " " + source.getAbsolutePath, source)
      else
        compiler(source, options.contains("bare"))
    } catch {
      case e: JavaScriptException => {

        val line = """.*on line ([0-9]+).*""".r
        val error = e.getValue.asInstanceOf[Scriptable]

        throw ScriptableObject.getProperty(error, "message").toString match {
          case msg @ line(l) => AssetCompilationException(Some(source), msg, Some(Integer.parseInt(l)), None)
          case msg => AssetCompilationException(Some(source), msg, None, None)
        }

      }
    }
  }

}
