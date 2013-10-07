package play.templates.test

import org.specs2.mutable._
import play.templates._
import java.io._
import org.specs2.specification.Scope
import scala.tools.nsc.util.ScalaClassLoader
import scalax.file.Path

import scala.language.reflectiveCalls

object TemplateCompilerSpec extends Specification {

  import Helper._

  val sourceDir = new File("src/templates-compiler/src/test/templates")
  val generatedDir = new File("src/templates-compiler/target/test/generated-templates")
  val generatedClasses = new File("src/templates-compiler/target/test/generated-classes")
  scalax.file.Path(generatedDir).deleteRecursively()
  scalax.file.Path(generatedClasses).deleteRecursively()
  scalax.file.Path(generatedClasses).createDirectory()

  abstract class Context extends Scope {
    val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
    def compileTemplate[T <: AnyRef](templateName: String) =
      helper.compileTemplate[T](s"$templateName.scala.html", s"html.$templateName")
  }

  "The template compiler" should {
    "compile the following successfully" >> {

      "real template" in new Context {
        type ExpectedType = { def apply(name: String, items: List[String])(repeat: Int): Html }
        val template = compileTemplate[ExpectedType]("real")

        template("World", List("A", "B"))(4).toString.trim must beLike {
          case html =>
            {
              if (html.contains("<h1>Hello World</h1>") &&
                html.contains("You have 2 items") &&
                html.contains("EA") &&
                html.contains("EB")) ok else ko
            }
        }
      }

      "static template" in new Context {
        type ExpectedType = { def apply(): Html }
        val template = compileTemplate[ExpectedType]("static")
        template().toString.trim must be_==(
          "<h1>It works</h1>")
      }

      "patternMatching template" in new Context {
        type ExpectedType = { def apply(test: String): Html }
        val template = compileTemplate[ExpectedType]("patternMatching")
        template("12345").toString.trim must be_==(
          """|@test
        	 |@test.length
        	 |@test.length.toInt
        	 |
             |@(test)
        	 |@(test.length)
        	 |@(test.length + 1)
        	 |@(test.+(3))
        	 |
        	 |5 match @test.length""".stripMargin)
      }

      "hello template" in new Context {
        type ExpectedType = { def apply(name: String): Html }
        val template = compileTemplate[ExpectedType]("hello")
        template("World").toString.trim must be_==(
          "<h1>Hello World!</h1><h1>xml</h1>")
      }

      "set template" in new Context {
        type ExpectedType = { def apply(test: Set[String]): Html; }
        val template = compileTemplate[ExpectedType]("set")
        template(Set("first", "second", "third")).toString.trim
          .replace("\n", "")
          .replaceAll("\\s+", "") must be_==("firstsecondthird")
      }

      "forSomeType template" in new Context {
        type ExpectedType = { def apply(seq: Seq[_]): Html }
        val template = compileTemplate[ExpectedType]("forSomeType")
        template(Seq("first", "second", "third")).toString.trim must
          be_==("first,second,third")
      }

      "defaultValue template" in new Context {
        type ExpectedType = { def apply(arg: String = "<see template>"): Html }
        val template = compileTemplate[ExpectedType]("defaultValue")
        template().toString.trim must
          be_==("1")

        template("2").toString.trim must
          be_==("2")
      }

      "onlyImplicit template" in new Context {
        type ExpectedType = { def apply(implicit test: String): Html }
        val template = compileTemplate[ExpectedType]("onlyImplicit")

        implicit val s: String = "test1"
        (template.apply).toString.trim must
          be_==("test1")

        template("test2").toString.trim must
          be_==("test2")
      }
    }
    "fail compilation for error.scala.html" in new Context {
      compileTemplate[(() => Html)]("error") must throwA[CompilationError].like {
        case CompilationError(_, 2, 12) => ok
        case _ => ko
      }
    }
    "produce a template that is a function" in new Context {
      type ExpectedType = {
        def apply(arg1: String, arg2: => String, arg3: String*)(arg4: String, arg5: => String, arg6: String*)(arg7: List[String], arg8: => List[String], arg9: List[String]*)(implicit arg10: String): Html
      }

      val template = compileTemplate[ExpectedType]("function")

      val testFile = new File(generatedDir, "TestFunction.scala")
      Path(testFile).write(
        """|object TestFunction {
           |  val withoutImplicits:(String, => String, String *) => (String, => String, String *) => (List[String], => List[String], List[String] *) => (String) => play.templates.test.Helper.HtmlFormat.Appendable = 
           |    html.function
           |
           |  val withImplicits:(String, => String, String *) => (String, => String, String *) => (List[String], => List[String], List[String] *) => play.templates.test.Helper.HtmlFormat.Appendable = {
           |    implicit val s = "test"
           |    html.function
           |  }
    	   |}""".stripMargin)
      helper.compileFile(testFile)
      val obj = helper.loadObject[AnyRef]("TestFunction")

      template("one", "two", "three", "four")("five", "six", "seven", "eight")(List("nine"), List("ten"), List("eleven"), List("twelve"))("test1").toString.trim must
        be_==("""one,two,three,four,five,six,seven,eight,nine,ten,eleven,twelve,test1""")

      implicit val s: String = "test2"
      template("one", "two", "three", "four")("five", "six", "seven", "eight")(List("nine"), List("ten"), List("eleven"), List("twelve")).toString.trim must
        be_==("""one,two,three,four,five,six,seven,eight,nine,ten,eleven,twelve,test2""")
    }
  }
}

object Helper {

  case class Html(text: String) extends Appendable[Html] {
    val buffer = new StringBuilder(text)
    def +(other: Html) = {
      buffer.append(other.buffer)
      this
    }
    override def toString = buffer.toString
  }

  object HtmlFormat extends Format[Html] {
    def raw(text: String) = Html(text)
    def escape(text: String) = Html(text.replace("<", "&lt;"))
  }

  case class CompilationError(message: String, line: Int, column: Int) extends RuntimeException(message)

  class CompilerHelper(sourceDir: File, generatedDir: File, generatedClasses: File) {
    import scala.tools.nsc.Global
    import scala.tools.nsc.Settings
    import scala.tools.nsc.reporters.ConsoleReporter
    import scala.reflect.internal.util.Position
    import scala.collection.mutable

    import java.net._

    val templateCompiler = ScalaTemplateCompiler

    val classloader = new ScalaClassLoader.URLClassLoader(Array(generatedClasses.toURI.toURL), Class.forName("play.templates.ScalaTemplateCompiler").getClassLoader)

    // A list of the compile errors from the most recent compiler run
    val compileErrors = new mutable.ListBuffer[CompilationError]

    val compiler = {

      def additionalClassPathEntry: Option[String] = Some(
        Class.forName("play.templates.ScalaTemplateCompiler").getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).mkString(":"))

      val settings = new Settings
      val scalaObjectSource = Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource

      // is null in Eclipse/OSGI but luckily we don't need it there
      if (scalaObjectSource != null) {
        val compilerPath = Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation
        val libPath = scalaObjectSource.getLocation
        val pathList = List(compilerPath, libPath)
        val origBootclasspath = settings.bootclasspath.value
        settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) mkString File.pathSeparator
        settings.outdir.value = generatedClasses.getAbsolutePath
      }

      val compiler = new Global(settings, new ConsoleReporter(settings) {
        override def printMessage(pos: Position, msg: String) = {
          compileErrors.append(CompilationError(msg, pos.line, pos.point))
        }
      })

      compiler
    }

    def compile(files: List[String]) = {
      val run = new compiler.Run

      run.compile(files)
    }

    def compileFile(file: File) = {
      compile(List(file.getAbsolutePath))

      compileErrors.headOption.foreach(throw _)
    }

    def compileTemplate[T <: AnyRef](templateName: String, className: String): T = {
      val templateFile = new File(sourceDir, templateName)
      val Some(generated) = templateCompiler.compile(templateFile, sourceDir, generatedDir, "play.templates.test.Helper.HtmlFormat")

      val mapper = GeneratedSource(generated)

      compile(List(generated.getAbsolutePath))

      compileErrors.headOption.foreach {
        case x @ CompilationError(msg, line, column) => {
          throw CompilationError(msg, mapper.mapLine(line), mapper.mapPosition(column))
        }
      }

      loadObject(className)
    }

    def loadObject[T <: AnyRef](className: String): T = {
      val obj =
        classloader
          .tryToLoadClass[T](className + "$")
          .map(_.getDeclaredField("MODULE$").get(null))
          .getOrElse(throw new Exception(s"Class $className not found"))

      obj.asInstanceOf[T]
    }
  }
}
