package play.templates

//Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
import java.io.File
import java.security.CodeSource

import scala.Option.option2Iterable
import scala.language.postfixOps
import scala.language.reflectiveCalls
import scala.reflect.internal.Flags
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.internal.util.Position
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.Global
import scala.tools.nsc.interactive.Response
import scala.tools.nsc.reporters.ConsoleReporter

import scalax.file.Path

trait TemplateGenerator { self: TemplateElements =>

  def generateCode(packageName: String, name: String, root: Template, resultType: String, formatterType: String, additionalImports: String) = {
    val (functionType, renderMethod, fMethod, toFunctionImplicitMethods, templateType) =
      TemplateAsFunctionCompiler.getFunctionMapping(name, root.params.str, resultType)

    val generated =
      Nil :+
        s"""|package $packageName
		    |
            |import scala.language.implicitConversions
		    |import play.templates._
		    |import play.templates.TemplateMagic._
		    |
		    |$additionalImports
		    |/*${root.comment.map(_.msg).getOrElse("")}*/
		    |object $name extends BaseScalaTemplate[$resultType, Format[$resultType]]($formatterType) with $templateType {
		    |
		    |  /*${root.comment.map(_.msg).getOrElse("")}*/
		    |  def apply""" :+ Source(root.params.str, root.params.pos) :+ s""":$resultType = {
		    |    _display_ {""" :+ templateCode(root, resultType) :+ s"""}
		    |  }
		    |
		    |  $toFunctionImplicitMethods
		    |    
		    |  $renderMethod
		    |
		    |  @scala.deprecated("The template itself now be typed as a function", "2.2.1")
		    |  $fMethod
		    |
		    |  def ref: this.type = this
		    |}"""

    generated.map {
      case string: String => string.stripMargin
      case other => other
    }
  }

  def templateCode(template: Template, resultType: String): Seq[Any] = {

    val defs = (template.sub ++ template.defs).map { i =>
      i match {
        case t: Template if t.name == "" => templateCode(t, resultType)
        case t: Template => {
          Nil :+ (if (t.name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(t.name.str, t.name.pos) :+ Source(t.params.str, t.params.pos) :+ ":" :+ resultType :+ " = {_display_(" :+ templateCode(t, resultType) :+ ")};"
        }
        case Def(name, params, block) => {
          Nil :+ (if (name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(name.str, name.pos) :+ Source(params.str, params.pos) :+ " = {" :+ block.code :+ "};"
        }
      }
    }

    val imports = template.imports.map(_.code).mkString("\n")

    Nil :+ imports :+ "\n" :+ defs :+ "\n" :+ "Seq[Any](" :+ visit(template.content, Nil) :+ ")"
  }

  @deprecated("use generateFinalTemplate with 8 parameters instead", "Play 2.1")
  def generateFinalTemplate(template: File, packageName: String, name: String, root: Template, resultType: String, formatterType: String, additionalImports: String): String = {
    generateFinalTemplate(template.getAbsolutePath, Path(template).byteArray, packageName, name, root, resultType, formatterType, additionalImports)
  }

  def generateFinalTemplate(absolutePath: String, contents: Array[Byte], packageName: String, name: String, root: Template, resultType: String, formatterType: String, additionalImports: String): String = {
    val generated = generateCode(packageName, name, root, resultType, formatterType, additionalImports)

    Source.finalSource(absolutePath, contents, generated, Hash(contents, additionalImports))
  }

  def visit(elem: Seq[TemplateTree], previous: Seq[Any]): Seq[Any] = {
    elem match {
      case head :: tail =>
        val tripleQuote = "\"\"\""
        visit(tail, head match {
          case p @ Plain(text) => (if (previous.isEmpty) Nil else previous :+ ",") :+ "format.raw" :+ Source("(", p.pos) :+ tripleQuote :+ text :+ tripleQuote :+ ")"
          case Comment(msg) => previous
          case Display(exp) => (if (previous.isEmpty) Nil else previous :+ ",") :+ "_display_(Seq[Any](" :+ visit(Seq(exp), Nil) :+ "))"
          case ScalaExp(parts) => previous :+ parts.map {
            case s @ Simple(code) => Source(code, s.pos)
            case b @ Block(whitespace, args, content) if (content.forall(_.isInstanceOf[ScalaExp])) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ visit(content, Nil) :+ "}"
            case b @ Block(whitespace, args, content) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ "_display_(Seq[Any](" :+ visit(content, Nil) :+ "))}"
          }
        })
      case Nil => previous
    }
  }

  object TemplateAsFunctionCompiler {

    // Note, the presentation compiler is not thread safe, all access to it must be synchronized.  If access to it
    // is not synchronized, then weird things happen like FreshRunReq exceptions are thrown when multiple sub projects
    // are compiled (done in parallel by default by SBT).  So if adding any new methods to this object, make sure you
    // make them synchronized.

    import java.io.File
    import scala.tools.nsc.interactive.{ Response, Global }
    import scala.tools.nsc.io.AbstractFile
    import scala.tools.nsc.Settings
    import scala.tools.nsc.reporters.ConsoleReporter

    def getFunctionMapping(name: String, signature: String, returnType: String): (String, String, String, String, String) = synchronized {

      type Tree = PresentationCompiler.global.Tree
      type DefDef = PresentationCompiler.global.DefDef
      type TypeDef = PresentationCompiler.global.TypeDef
      type ValDef = PresentationCompiler.global.ValDef

      def findSignature(tree: Tree): Option[DefDef] = {
        tree match {
          case t: DefDef if t.name.toString == "signature" => Some(t)
          case t: Tree => t.children.flatMap(findSignature).headOption
        }
      }

      case class Param(name: String, tpe: String, isImplicit: Boolean, isByName: Boolean, defaultValue: String) {
        private val REPEATED = raw"^_root_\.scala\.<repeated>\[(.+?)\]$$".r
        private val SYNTHETIC = "<synthetic>"

        val hasDefaultValue = defaultValue != "<empty>"

        val (varargs, cleanType) =
          tpe.replaceAll(SYNTHETIC, "") match {
            case REPEATED(cleanType) => (true, cleanType)
            case cleanType => (false, cleanType)
          }

        val normalizedType =
          if (varargs) s"Array[$cleanType]"
          else cleanType

        val nameForCall = name + (if (varargs) ":_*" else "")
        val fullType =
          (if (isByName) " => " else "") +
            cleanType +
            (if (varargs) " *" else "")
        val nameAndNormalizedType = name + ":" + normalizedType
        val fullParam = name + ":" + fullType + (if (hasDefaultValue) " = " + defaultValue else "")
      }

      def toParam(param: ValDef): Param = {
        val defaultValue = param.rhs.toString
        val isByName = param.mods.hasFlag(Flags.BYNAMEPARAM)
        val isImplicit = param.mods.hasFlag(Flags.IMPLICIT)
        val tpe =
          if (isByName) param.tpt.children(1).toString
          else param.tpt.toString
        Param(param.name.toString, tpe, isImplicit, isByName, defaultValue)
      }

      val params =
        findSignature(
          PresentationCompiler.treeFrom("object FT { def signature" + signature + " }"))
          .get
          .vparamss
          .map(_.map(toParam))

      val allParams = params.flatten

      val functionType =
        "(" + params.map("(" + _.map(_.fullType).mkString(",") + ")").mkString(" => ") + " => " + returnType + ")"

      val parametersForCall =
        params.map("(" + _.map(_.nameForCall).mkString(",") + ")").mkString

      val renderMethod = {
        val singleParameterList =
          "(" + allParams.map(_.nameAndNormalizedType).mkString(",") + ")"

        s"def render$singleParameterList: $returnType = apply$parametersForCall"
      }

      val templateType = {
        val parameterCount = allParams.size
        val argumentTypes = allParams.map(_.normalizedType)
        val typeParameters = (argumentTypes :+ returnType).mkString(",")

        s"play.api.templates.Template$parameterCount[$typeParameters]"
      }

      val fMethod = {
        val closureParameters =
          params.map(group => "(" + group.map(_.name).mkString(",") + ")").mkString(" => ")
        s"def f:$functionType = $closureParameters => apply$parametersForCall"
      }

      val toFunctionImplicitMethods = {
        val hasImplicits = params.lastOption.map(_.headOption.map(_.isImplicit).getOrElse(false)).getOrElse(false)

        if (hasImplicits) {

          val applyParameters =
            params.map("(" + _.map(_.nameForCall).mkString(",") + ")").mkString

          val withoutImplicits = {

            val closureParameters =
              params.map("(" + _.map(_.name).mkString(",") + ")").mkString(" => ")

            s"""|implicit def toFunction(template:$name.type):$functionType = 
                |  { $closureParameters => apply$applyParameters }
                |""".stripMargin
          }

          val withImplicits = {
            val implicitParams =
              "(implicit " + params.last.map(_.fullParam).mkString(",") + ")"
            val typeWithoutImplicits =
              params.dropRight(1).map("(" + _.map(_.fullType).mkString(",") + ")")

            val onlyImplicits = typeWithoutImplicits.isEmpty
            val functionTypeWithoutImplicits =
              if (onlyImplicits) returnType
              else "(" + typeWithoutImplicits.mkString(" => ") + " => " + returnType + ")"

            val applyCall =
              if (onlyImplicits) "apply" + applyParameters
              else "apply _"

            s"""|implicit def toFunction(template:$name.type)$implicitParams:$functionTypeWithoutImplicits = 
            	|  $applyCall
                |""".stripMargin
          }

          withoutImplicits + withImplicits
        } else
          s"implicit def toFunction(template:$name.type):$functionType = apply _"
      }

      (functionType, renderMethod, fMethod, toFunctionImplicitMethods, templateType)
    }

    class CompilerInstance {

      def additionalClassPathEntry: Option[String] = None

      lazy val compiler = {

        val settings = new Settings

        val scalaObjectSource = Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource

        // is null in Eclipse/OSGI but luckily we don't need it there
        if (scalaObjectSource != null) {
          import java.security.CodeSource
          def toAbsolutePath(cs: CodeSource) = new File(cs.getLocation.toURI).getAbsolutePath
          val compilerPath = toAbsolutePath(Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource)
          val libPath = toAbsolutePath(scalaObjectSource)
          val pathList = List(compilerPath, libPath)
          val origBootclasspath = settings.bootclasspath.value
          settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) mkString File.pathSeparator
        }

        val compiler = new Global(settings, new ConsoleReporter(settings) {
          override def printMessage(pos: Position, msg: String) = ()
        })

        // Everything must be done on the compiler thread, because the presentation compiler is a fussy piece of work.
        compiler.ask(() => new compiler.Run)

        compiler
      }
    }

    trait TreeCreationMethods {

      val global: scala.tools.nsc.interactive.Global

      val randomFileName = {
        val r = new java.util.Random
        () => "file" + r.nextInt
      }

      def treeFrom(src: String): global.Tree = {
        val file = new BatchSourceFile(randomFileName(), src)
        treeFrom(file)
      }

      def treeFrom(file: SourceFile): global.Tree = {
        import tools.nsc.interactive.Response

        type Scala29Compiler = {
          def askParsedEntered(file: SourceFile, keepLoaded: Boolean, response: Response[global.Tree]): Unit
          def askType(file: SourceFile, forceReload: Boolean, respone: Response[global.Tree]): Unit
        }

        val newCompiler = global.asInstanceOf[Scala29Compiler]

        val r1 = new Response[global.Tree]
        newCompiler.askParsedEntered(file, true, r1)
        r1.get.left.toOption.getOrElse(throw r1.get.right.get)
      }

    }

    object CompilerInstance extends CompilerInstance

    object PresentationCompiler extends TreeCreationMethods {
      val global = CompilerInstance.compiler

      def shutdown() {
        global.askShutdown()
      }
    }
  }
}