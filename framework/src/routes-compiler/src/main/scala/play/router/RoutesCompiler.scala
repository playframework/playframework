package play.router

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

/**
 * provides a compiler for routes
 */
object RoutesCompiler {

  case class HttpVerb(value: String) {
    override def toString = value
  }
  case class HandlerCall(packageName: String, controller: String, instantiate: Boolean, method: String, parameters: Option[Seq[Parameter]]) extends Positional {
    val dynamic = if (instantiate) "@" else ""
    override def toString = dynamic + packageName + "." + controller + dynamic + "." + method + parameters.map { params =>
      "(" + params.mkString(", ") + ")"
    }.getOrElse("")
  }
  case class Parameter(name: String, typeName: String, fixed: Option[String], default: Option[String]) extends Positional {
    override def toString = name + ":" + typeName + fixed.map(" = " + _).getOrElse("") + default.map(" ?= " + _).getOrElse("")
  }

  sealed trait Rule extends Positional

  case class Route(verb: HttpVerb, path: PathPattern, call: HandlerCall, comments: List[Comment] = List()) extends Rule
  case class Include(prefix: String, router: String) extends Rule

  case class Comment(comment: String)


  object Hash {

    def apply(bytes: Array[Byte]): String = {
      import java.security.MessageDigest
      val digest = MessageDigest.getInstance("SHA-1")
      digest.reset()
      digest.update(bytes)
      digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
    }

  }

  // --- Parser

  private[router] class RouteFileParser extends JavaTokenParsers {

    override def skipWhitespace = false
    override val whiteSpace = """[ \t]+""".r

    override def phrase[T](p: Parser[T]) = new Parser[T] {
      lastNoSuccess = null
      def apply(in: Input) = p(in) match {
        case s @ Success(out, in1) =>
          if (in1.atEnd)
            s
          else if (lastNoSuccess == null || lastNoSuccess.next.pos < in1.pos)
            Failure("end of input expected", in1)
          else
            lastNoSuccess
        case _ => lastNoSuccess
      }
    }

    def EOF: util.matching.Regex = "\\z".r

    def namedError[A](p: Parser[A], msg: String): Parser[A] = Parser[A] { i =>
      p(i) match {
        case Failure(_, in) => Failure(msg, in)
        case o => o
      }
    }

    def several[T](p: => Parser[T]): Parser[List[T]] = Parser { in =>
      import scala.collection.mutable.ListBuffer
      val elems = new ListBuffer[T]
      def continue(in: Input): ParseResult[List[T]] = {
        val p0 = p // avoid repeatedly re-evaluating by-name parser
        @scala.annotation.tailrec
        def applyp(in0: Input): ParseResult[List[T]] = p0(in0) match {
          case Success(x, rest) => elems += x; applyp(rest)
          case Failure(_, _) => Success(elems.toList, in0)
          case err: Error => err
        }
        applyp(in)
      }
      continue(in)
    }

    def separator: Parser[String] = namedError(whiteSpace, "Whitespace expected")

    def ignoreWhiteSpace: Parser[Option[String]] = opt(whiteSpace)

    // This won't be needed when we upgrade to Scala 2.11, we will then be able to use JavaTokenParser.ident:
    // https://github.com/scala/scala/pull/1466
    def javaIdent: Parser[String] = """\p{javaJavaIdentifierStart}\p{javaJavaIdentifierPart}*""".r

    def identifier: Parser[String] = namedError(javaIdent, "Identifier expected")

    def end: util.matching.Regex = """\s*""".r

    def comment: Parser[Comment] = "#" ~> ".*".r ^^ {
      case c => Comment(c)
    }

    def newLine: Parser[String] = namedError((("\r"?) ~> "\n"), "End of line expected")

    def blankLine: Parser[Unit] = ignoreWhiteSpace ~> newLine ^^ { case _ => () }

    def parentheses: Parser[String] = {
      "(" ~ (several((parentheses | not(")") ~> """.""".r))) ~ commit(")") ^^ {
        case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
      }
    }

    def brackets: Parser[String] = {
      "[" ~ (several((parentheses | not("]") ~> """.""".r))) ~ commit("]") ^^ {
        case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
      }
    }

    def string: Parser[String] = {
      "\"" ~ (several((parentheses | not("\"") ~> """.""".r))) ~ commit("\"") ^^ {
        case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
      }
    }

    def multiString: Parser[String] = {
      "\"\"\"" ~ (several((parentheses | not("\"\"\"") ~> """.""".r))) ~ commit("\"\"\"") ^^ {
        case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
      }
    }

    def httpVerb: Parser[HttpVerb] = namedError("GET" | "POST" | "PUT" | "PATCH" | "HEAD" | "DELETE" | "OPTIONS", "HTTP Verb expected") ^^ {
      case v => HttpVerb(v)
    }

    def singleComponentPathPart: Parser[DynamicPart] = (":" ~> identifier) ^^ {
      case name => DynamicPart(name, """[^/]+""")
    }

    def multipleComponentsPathPart: Parser[DynamicPart] = ("*" ~> identifier) ^^ {
      case name => DynamicPart(name, """.+""")
    }

    def regexComponentPathPart: Parser[DynamicPart] = "$" ~> identifier ~ ("<" ~> (not(">") ~> """[^\s]""".r +) <~ ">" ^^ { case c => c.mkString }) ^^ {
      case name ~ regex => DynamicPart(name, regex)
    }

    def staticPathPart: Parser[StaticPart] = (not(":") ~> not("*") ~> not("$") ~> """[^\s]""".r +) ^^ {
      case chars => StaticPart(chars.mkString)
    }

    def path: Parser[PathPattern] = "/" ~ ((positioned(singleComponentPathPart) | positioned(multipleComponentsPathPart) | positioned(regexComponentPathPart) | staticPathPart) *) ^^ {
      case _ ~ parts => PathPattern(parts)
    }

    def parameterType: Parser[String] = ":" ~> ignoreWhiteSpace ~> rep1sep(identifier, ".") ~ opt(brackets) ^^ {
      case t ~ g => t.mkString(".") + g.getOrElse("")
    }

    def expression: Parser[String] = (multiString | string | parentheses | brackets | """[^),?=\n]""".r +) ^^ {
      case p => p.mkString
    }

    def parameterFixedValue: Parser[String] = "=" ~ ignoreWhiteSpace ~ expression ^^ {
      case a ~ _ ~ b => a + b
    }

    def parameterDefaultValue: Parser[String] = "?=" ~ ignoreWhiteSpace ~ expression ^^ {
      case a ~ _ ~ b => a + b
    }

    def parameter: Parser[Parameter] = (identifier <~ ignoreWhiteSpace) ~ opt(parameterType) ~ (ignoreWhiteSpace ~> opt(parameterDefaultValue | parameterFixedValue)) ^^ {
      case name ~ t ~ d => Parameter(name, t.getOrElse("String"), d.filter(_.startsWith("=")).map(_.drop(1)), d.filter(_.startsWith("?")).map(_.drop(2)))
    }

    def parameters: Parser[List[Parameter]] = "(" ~> repsep(ignoreWhiteSpace ~> positioned(parameter) <~ ignoreWhiteSpace, ",") <~ ")"

    // Absolute method consists of a series of Java identifiers representing the package name, controller and method.
    // Since the Scala parser is greedy, we can't easily extract this out, so just parse at least 3
    def absoluteMethod: Parser[List[String]] = namedError(javaIdent ~ "." ~ javaIdent ~ "." ~ rep1sep(javaIdent, ".") ^^ {
      case first ~ _ ~ second ~ _ ~ rest => first :: second :: rest
    }, "Controller method call expected")

    def call: Parser[HandlerCall] = opt("@") ~ absoluteMethod ~ opt(parameters) ^^ {
      case instantiate ~ absMethod ~ parameters =>
        {
          val (packageParts, classAndMethod) = absMethod.splitAt(absMethod.size - 2)
          val packageName = packageParts.mkString(".")
          val className = classAndMethod(0)
          val methodName = classAndMethod(1)
          val dynamic = !instantiate.isEmpty
          HandlerCall(packageName, className, dynamic, methodName, parameters)
        }
    }

    def router: Parser[String] = rep1sep(identifier, ".") ^^ {
      case parts => parts.mkString(".")
    }

    def route = httpVerb ~! separator ~ path ~ separator ~ positioned(call) ~ ignoreWhiteSpace ^^ {
      case v ~ _ ~ p ~ _ ~ c ~ _ => Route(v, p, c)
    }

    def include = "->" ~! separator ~ path ~ separator ~ router ~ ignoreWhiteSpace ^^ {
      case _ ~ _ ~ p ~ _ ~ r ~ _ => Include(p.toString, r)
    }

    def sentence: Parser[Product with Serializable] = namedError((comment | positioned(include) | positioned(route)), "HTTP Verb (GET, POST, ...), include (->) or comment (#) expected") <~ (newLine | EOF)

    def parser: Parser[List[Rule]] = phrase((blankLine | sentence *) <~ end) ^^ {
      case routes => 
        routes.reverse.foldLeft(List[(Option[Rule],List[Comment])]()) {
          case (s,r@Route(_,_,_,_)) => (Some(r),List()) :: s
          case (s,i@Include(_,_)) => (Some(i),List()) :: s
          case ( s, c@()) => (None, List()) :: s
          case ( (r,comments) :: others, c@Comment(_)) => (r, c:: comments) :: others
          case (s,_) => s
        }.collect {
          case (Some(r@Route(_,_,_,_)), comments) => r.copy(comments = comments).setPos(r.pos)
          case (Some(i@Include(_,_)),_) => i
        }
    }

    def parse(text: String): ParseResult[List[Rule]] = {
      parser(new CharSequenceReader(text))
    }
  }


  import scalax.file._
  import java.io.File

  case class RoutesCompilationError(source: File, message: String, line: Option[Int], column: Option[Int]) extends RuntimeException(message)

  case class GeneratedSource(file: File) {

    val lines = if (file.exists) Path(file).string.split('\n').toList else Nil
    val source = lines.headOption.filter(_.startsWith("// @SOURCE:")).map(m => Path.fromString(m.trim.drop(11)))

    def isGenerated: Boolean = source.isDefined

    def sync(): Boolean = {
      if (!source.get.exists) file.delete() else false
    }

    def needsRecompilation: Boolean = {
      val hash = lines.find(_.startsWith("// @HASH:")).map(m => m.trim.drop(9)).getOrElse("")
      source.filter(_.exists).map { p =>
        Hash(p.byteArray) != hash
      }.getOrElse(true)
    }

    def mapLine(generatedLine: Int): Option[Int] = {
      lines.take(generatedLine).reverse.collect {
        case l if l.startsWith("// @LINE:") => Integer.parseInt(l.trim.drop(9))
      }.headOption
    }

  }

  object MaybeGeneratedSource {

    def unapply(source: File): Option[GeneratedSource] = {
      val generated = GeneratedSource(source)
      if (generated.isGenerated) {
        Some(generated)
      } else {
        None
      }
    }

  }


  def compile(file: File, generatedDir: File, additionalImports: Seq[String]) {

    val generated = GeneratedSource(new File(generatedDir, "routes_routing.scala"))

    if (generated.needsRecompilation) {

      val parser = new RouteFileParser
      val routeFile = Path(file).toAbsolute
      val routesContent = routeFile.string

      (parser.parse(routesContent) match {
        case parser.Success(parsed, _) => generate(routeFile, parsed, additionalImports)
        case parser.NoSuccess(message, in) => {
          throw RoutesCompilationError(file, message, Some(in.pos.line), Some(in.pos.column))
        }
      }).foreach { item =>
        Path(new File(generatedDir, item._1)).write(item._2)
      }

    }

  }

  /**
   * Precheck routes coherence or throw exceptions early
   */
  private def check(file: java.io.File, routes: List[Route]) {
    routes.foreach { route =>

      if (route.call.packageName.isEmpty) {
        throw RoutesCompilationError(
          file,
          "Missing package name",
          Some(route.call.pos.line),
          Some(route.call.pos.column))
      }

      if (route.call.controller.isEmpty) {
        throw RoutesCompilationError(
          file,
          "Missing Controller",
          Some(route.call.pos.line),
          Some(route.call.pos.column))
      }

      route.path.parts.collect {
        case part @ DynamicPart(name, regex) => {
          route.call.parameters.getOrElse(Nil).find(_.name == name).map { p =>
            if (p.fixed.isDefined || p.default.isDefined) {
              throw RoutesCompilationError(
                file,
                "It is not allowed to specify a fixed or default value for parameter: '" + name + "' extracted from the path",
                Some(p.pos.line),
                Some(p.pos.column))
            }
            try {
              java.util.regex.Pattern.compile(regex)
            } catch {
              case e: Exception => {
                throw RoutesCompilationError(
                  file,
                  e.getMessage,
                  Some(part.pos.line),
                  Some(part.pos.column))
              }
            }
          }.getOrElse {
            throw RoutesCompilationError(
              file,
              "Missing parameter in call definition: " + name,
              Some(part.pos.line),
              Some(part.pos.column))
          }
        }
      }

    }
  }

  private def markLines(routes: Rule*): String = {
    routes.map("// @LINE:" + _.pos.line).reverse.mkString("\n")
  }

  /**
   * Generate the actual Scala code for this router
   */
  private def generate(file: Path, rules: List[Rule], additionalImports: Seq[String]): Seq[(String, String)] = {

    check(new File(file.path), rules.collect { case r: Route => r });

    val (path, hash, date) = (file.path.replace(File.separator, "/"), Hash(file.byteArray), new java.util.Date().toString)

    Seq(("routes_reverseRouting.scala",
      """ |// @SOURCE:%s
          |// @HASH:%s
          |// @DATE:%s
          |
          |%s
          |import play.core._
          |import play.core.Router._
          |import play.core.j._
          |
          |import play.api.mvc._
          |%s
          |
          |import Router.queryString
          |
          |%s
          |
          |%s
          |
          |%s
      """.stripMargin.format(
        path,
        hash,
        date,
        Option(file.name).filter(_.endsWith(".routes")).map(_.dropRight(".routes".size)).map("import " + _ + ".Routes").getOrElse(""),
        additionalImports.map("import " + _).mkString("\n"),
        reverseRouting(rules.collect { case r: Route => r }),
        javaScriptReverseRouting(rules.collect { case r: Route => r }),
        refReverseRouting(rules.collect { case r: Route => r })
      )
    ),
      ("routes_routing.scala",
        """ |// @SOURCE:%s
            |// @HASH:%s
            |// @DATE:%s
            |%s
            |
            |import play.core._
            |import play.core.Router._
            |import play.core.j._
            |
            |import play.api.mvc._
            |%s
            |
            |import Router.queryString
            |
            |object Routes extends Router.Routes {
            |
            |private var _prefix = "/"
            |
            |def setPrefix(prefix: String) {
            |  _prefix = prefix  
            |  List[(String,Routes)](%s).foreach {
            |    case (p, router) => router.setPrefix(prefix + (if(prefix.endsWith("/")) "" else "/") + p)
            |  }
            |}
            |
            |def prefix = _prefix
            |
            |lazy val defaultPrefix = { if(Routes.prefix.endsWith("/")) "" else "/" } 
            |
            |%s 
            |    
            |def routes:PartialFunction[RequestHeader,Handler] = {        
            |%s
            |}
            |    
            |}
        """.stripMargin.format(
          path,
          hash,
          date,
          Option(file.name).filter(_.endsWith(".routes")).map(_.dropRight(".routes".size)).map("package " + _).getOrElse(""),
          additionalImports.map("import " + _).mkString("\n"),
          rules.collect { case Include(p, r) => "(\"" + p + "\"," + r + ")" }.mkString(","),
          routeDefinitions(rules),
          routing(rules)
        )
      )) ++ {

        // Generate Java wrappers

        rules.collect { case r: Route => r }.groupBy(_.call.packageName).map {
          case (packageName, routes) => {

            (packageName.replace(".", "/") + "/routes.java") -> {

              """ |// @SOURCE:%s
                  |// @HASH:%s
                  |// @DATE:%s
                  |
                  |package %s;
                  |
                  |public class routes {
                  |%s
                  |public static class javascript {
                  |%s    
                  |}   
                  |public static class ref {
                  |%s    
                  |} 
                  |}
              """.stripMargin.format(
                path, hash, date,
                packageName,

                routes.groupBy(_.call.controller).map {
                  case (controller, routes) => {
                    "public static final " + packageName + ".Reverse" + controller + " " + controller + " = new " + packageName + ".Reverse" + controller + "();"
                  }
                }.mkString("\n"),

                routes.groupBy(_.call.controller).map {
                  case (controller, _) => {
                    "public static final " + packageName + ".javascript.Reverse" + controller + " " + controller + " = new " + packageName + ".javascript.Reverse" + controller + "();"
                  }
                }.mkString("\n"),

                routes.groupBy(_.call.controller).map {
                  case (controller, _) => {
                    "public static final " + packageName + ".ref.Reverse" + controller + " " + controller + " = new " + packageName + ".ref.Reverse" + controller + "();"
                  }
                }.mkString("\n")

              )

            }

          }
        }

      }

  }

  /**
   * Generate the reverse routing operations
   */
  def javaScriptReverseRouting(routes: List[Route]): String = {

    routes.groupBy(_.call.packageName).map {
      case (packageName, routes) => {

        """
            |%s
            |package %s.javascript {
            |%s
            |}
        """.stripMargin.format(
          markLines(routes: _*),
          packageName,

          routes.groupBy(_.call.controller).map {
            case (controller, routes) =>
              """
                  |%s
                  |class Reverse%s {
                  |    
                  |%s
                  |    
                  |}
              """.stripMargin.format(
                markLines(routes: _*),

                // alias
                controller.replace(".", "_"),

                // reverse method
                routes.groupBy(r => r.call.method -> r.call.parameters.getOrElse(Nil).map(p => p.typeName)).map {
                  case ((m, _), routes) =>

                    assert(routes.size > 0, "Empty routes set???")

                    val parameters = routes(0).call.parameters.getOrElse(Nil)

                    val reverseParameters = parameters.zipWithIndex.filterNot {
                      case (p, i) => {
                        val fixeds = routes.map(_.call.parameters.get(i).fixed).distinct
                        fixeds.size == 1 && fixeds(0) != None
                      }
                    }

                    def genCall(route: Route, localNames: Map[String, String] = Map()) = "      return _wA({method:\"%s\", url:%s%s})".format(
                      route.verb.value,
                      "\"\"\"\" + Routes.prefix + " + { if (route.path.parts.isEmpty) "" else "{ Routes.defaultPrefix } + " } + "\"\"\"\"" + route.path.parts.map {
                        case StaticPart(part) => " + \"" + part + "\""
                        case DynamicPart(name, _) => {
                          route.call.parameters.getOrElse(Nil).find(_.name == name).map { param =>
                            " + (\"\"\" + implicitly[PathBindable[" + param.typeName + "]].javascriptUnbind + \"\"\")" + """("""" + param.name + """", """ + localNames.get(param.name).getOrElse(param.name) + """)"""
                          }.getOrElse {
                            throw new Error("missing key " + name)
                          }
                        }
                      }.mkString,

                      {
                        val queryParams = route.call.parameters.getOrElse(Nil).filterNot { p =>
                          p.fixed.isDefined ||
                            route.path.parts.collect {
                              case DynamicPart(name, _) => name
                            }.contains(p.name)
                        }

                        if (queryParams.size == 0) {
                          ""
                        } else {
                          """ + _qS([%s])""".format(
                            queryParams.map { p =>
                              ("(\"\"\" + implicitly[QueryStringBindable[" + p.typeName + "]].javascriptUnbind + \"\"\")" + """("""" + p.name + """", """ + localNames.get(p.name).getOrElse(p.name) + """)""") -> p
                            }.map {
                              case (u, Parameter(name, typeName, None, Some(default))) => """(""" + localNames.get(name).getOrElse(name) + " == null ? \"\"\" +  implicitly[JavascriptLitteral[" + typeName + "]].to(" + default + ") + \"\"\" : " + u + ")"
                              case (u, Parameter(name, typeName, None, None)) => u
                            }.mkString(", "))

                        }

                      })

                    routes match {

                      case Seq(route) => {
                        """
                            |%s
                            |def %s : JavascriptReverseRoute = JavascriptReverseRoute(
                            |   "%s",
                            |   %s
                            |      function(%s) {
                            |%s
                            |      }
                            |   %s
                            |)
                        """.stripMargin.format(
                          markLines(route),
                          route.call.method,
                          packageName + "." + controller + "." + route.call.method,
                          "\"\"\"",
                          reverseParameters.map(_._1.name).mkString(","),
                          genCall(route),
                          "\"\"\"")
                      }

                      case Seq(route, routes @ _*) => {
                        """
                            |%s
                            |def %s : JavascriptReverseRoute = JavascriptReverseRoute(
                            |   "%s",
                            |   %s
                            |      function(%s) {
                            |%s
                            |      }
                            |   %s
                            |)
                        """.stripMargin.format(
                          markLines((route +: routes): _*),
                          route.call.method,
                          packageName + "." + controller + "." + route.call.method,
                          "\"\"\"",
                          reverseParameters.map(_._1.name).mkString(", "),

                          // route selection
                          (route +: routes).map { route =>

                            val localNames = reverseParameters.map {
                              case (lp, i) => route.call.parameters.get(i).name -> lp.name
                            }.toMap

                            "      if (%s) {\n%s\n      }".format(

                              // Fixed constraints
                              Option(route.call.parameters.getOrElse(Nil).filter { p =>
                                localNames.contains(p.name) && p.fixed.isDefined
                              }.map { p =>
                                p.name + " == \"\"\" + implicitly[JavascriptLitteral[" + p.typeName + "]].to(" + p.fixed.get + ") + \"\"\""
                              }).filterNot(_.isEmpty).map(_.mkString(" && ")).getOrElse("true"),

                              genCall(route, localNames))

                          }.mkString("\n"),

                          "\"\"\"")
                      }

                    }
                }.mkString("\n")
              )
          }.mkString("\n"))

      }
    }.mkString("\n")

  }

  /**
   * Generate the routing refs
   */
  def refReverseRouting(routes: List[Route]): String = {

    routes.groupBy(_.call.packageName).map {
      case (packageName, routes) => {

        """
                      |%s
                      |package %s.ref {
                      |%s
                      |}
                  """.stripMargin.format(
          markLines(routes: _*),
          packageName,

          routes.groupBy(_.call.controller).map {
            case (controller, routes) =>
              """
                              |%s
                              |class Reverse%s {
                              |    
                              |%s
                              |    
                              |}
                          """.stripMargin.format(
                markLines(routes: _*),

                // alias
                controller.replace(".", "_"),

                // reverse method
                routes.groupBy(r => (r.call.method, r.call.parameters.getOrElse(Nil).map(p => p.typeName))).map {
                  case ((m, _), routes) =>

                    assert(routes.size > 0, "Empty routes set???")

                    val route = routes(0)

                    val parameters = route.call.parameters.getOrElse(Nil)

                    val reverseSignature = parameters.map(p => p.name + ":" + p.typeName).mkString(", ")

                    val controllerCall = if (route.call.instantiate) {
                      "play.api.Play.maybeApplication.map(_.global).getOrElse(play.api.DefaultGlobal).getControllerInstance(classOf[" + packageName + "." + controller + "])." + route.call.method + "(" + { parameters.map(_.name).mkString(", ") } + ")"
                    } else {
                      packageName + "." + controller + "." + route.call.method + "(" + { parameters.map(_.name).mkString(", ") } + ")"
                    }

                    """
                          |%s
                          |def %s(%s): play.api.mvc.HandlerRef[_] = new play.api.mvc.HandlerRef(
                          |   %s, HandlerDef(this, "%s", "%s", %s, "%s", "%s",  Routes.prefix + "%s")
                          |)
                      """.stripMargin.format(
                      markLines(route),
                      route.call.method,
                      reverseSignature,
                      controllerCall,
                      packageName + "." + controller,
                      route.call.method,
                      "Seq(" + { parameters.map("classOf[" + _.typeName + "]").mkString(", ") } + ")",
                      route.verb,
                      "\"\"\""+route.comments.map(_.comment).mkString("\n")+"\"\"\"",
                      route.path
                      )

                }.mkString("\n")
              )
          }.mkString("\n"))

      }
    }.mkString("\n")
  }

  /**
   * Generate the reverse routing operations
   */
  def reverseRouting(routes: List[Route]): String = {

    routes.groupBy(_.call.packageName).map {
      case (packageName, routes) => {

        """
                      |%s
                      |package %s {
                      |%s
                      |}
                  """.stripMargin.format(
          markLines(routes: _*),
          packageName,

          routes.groupBy(_.call.controller).map {
            case (controller, routes) =>
              """
                              |%s
                              |class Reverse%s {
                              |    
                              |%s
                              |    
                              |}
                          """.stripMargin.format(
                markLines(routes: _*),

                // alias
                controller.replace(".", "_"),

                // reverse method
                routes.groupBy(r => (r.call.method, r.call.parameters.getOrElse(Nil).map(p => p.typeName))).map {
                  case ((m, _), routes) =>

                    assert(routes.size > 0, "Empty routes set???")

                    val parameters = routes(0).call.parameters.getOrElse(Nil)

                    val reverseParameters = parameters.zipWithIndex.filterNot {
                      case (p, i) => {
                        val fixeds = routes.map(_.call.parameters.get(i).fixed).distinct
                        fixeds.size == 1 && fixeds(0) != None
                      }
                    }

                    val reverseSignature = reverseParameters.map(p => p._1.name + ":" + p._1.typeName + {
                      Option(routes.map(_.call.parameters.get(p._2).default).distinct).filter(_.size == 1).flatMap(_.headOption).map {
                        case None => ""
                        case Some(default) => " = " + default
                      }.getOrElse("")
                    }).mkString(", ")

                    def genCall(route: Route, localNames: Map[String, String] = Map()) = """Call("%s", %s%s)""".format(
                      route.verb.value,
                      "Routes.prefix" + { if (route.path.parts.isEmpty) "" else """ + { Routes.defaultPrefix } + """ } + route.path.parts.map {
                        case StaticPart(part) => "\"" + part + "\""
                        case DynamicPart(name, _) => {
                          route.call.parameters.getOrElse(Nil).find(_.name == name).map { param =>
                            """implicitly[PathBindable[""" + param.typeName + """]].unbind("""" + param.name + """", """ + localNames.get(param.name).getOrElse(param.name) + """)"""
                          }.getOrElse {
                            throw new Error("missing key " + name)
                          }

                        }
                      }.mkString(" + "),

                      {
                        val queryParams = route.call.parameters.getOrElse(Nil).filterNot { p =>
                          p.fixed.isDefined ||
                            route.path.parts.collect {
                              case DynamicPart(name, _) => name
                            }.contains(p.name)
                        }

                        if (queryParams.size == 0) {
                          ""
                        } else {
                          """ + queryString(List(%s))""".format(
                            queryParams.map { p =>
                              ("""implicitly[QueryStringBindable[""" + p.typeName + """]].unbind("""" + p.name + """", """ + localNames.get(p.name).getOrElse(p.name) + """)""") -> p
                            }.map {
                              case (u, Parameter(name, typeName, None, Some(default))) => """if(""" + localNames.get(name).getOrElse(name) + """ == """ + default + """) None else Some(""" + u + """)"""
                              case (u, Parameter(name, typeName, None, None)) => "Some(" + u + ")"
                            }.mkString(", "))

                        }

                      })

                    routes match {

                      case Seq(route) => {
                        """
                                                    |%s
                                                    |def %s(%s): Call = {
                                                    |   %s
                                                    |}
                                                """.stripMargin.format(
                          markLines(route),
                          route.call.method,
                          reverseSignature,
                          genCall(route))
                      }

                      case Seq(route, routes @ _*) => {
                        """
                                                    |%s
                                                    |def %s(%s): Call = {
                                                    |   (%s) match {
                                                    |%s
                                                    |   }
                                                    |}
                                                """.stripMargin.format(
                          markLines((route +: routes): _*),
                          route.call.method,
                          reverseSignature,
                          reverseParameters.map(_._1.name + ": @unchecked").mkString(", "),

                          // route selection
                          (route +: routes).map { route =>

                            val localNames = reverseParameters.map {
                              case (lp, i) => route.call.parameters.get(i).name -> lp.name
                            }.toMap;

                            """ |%s
                                                            |case (%s) %s => %s
                                                        """.stripMargin.format(
                              markLines(route),
                              reverseParameters.map(_._1.name).mkString(", "),

                              // Fixed constraints
                              Option(route.call.parameters.getOrElse(Nil).filter { p =>
                                localNames.contains(p.name) && p.fixed.isDefined
                              }.map { p =>
                                p.name + " == " + p.fixed.get
                              }).filterNot(_.isEmpty).map("if " + _.mkString(" && ")).getOrElse("if true"),

                              genCall(route, localNames))

                          }.mkString("\n"))
                      }

                    }

                }.mkString("\n")
              )
          }.mkString("\n")
        )
      }
    }.mkString("\n")

  }

  /**
   * Generate the routes definitions
   */
  def routeDefinitions(rules: List[Rule]): String = {
    rules.zipWithIndex.map {
      case (r @ Route(_, _, _, _), i) =>
        """
          |%s
          |private[this] lazy val %s%s = Route("%s", %s)
        """.stripMargin.format(
          markLines(r),
          r.call.packageName.replace(".", "_") + "_" + r.call.controller.replace(".", "_") + "_" + r.call.method,
          i,
          r.verb.value,
          "PathPattern(List(StaticPart(Routes.prefix)" + { if (r.path.parts.isEmpty) "" else """,StaticPart(Routes.defaultPrefix),""" } + r.path.parts.map(_.toString).mkString(",") + "))")
      case (r @ Include(_, _), i) =>
        """
          |%s
          |lazy val %s%s = Include(%s)
        """.stripMargin.format(
          markLines(r),
          r.router.replace(".", "_"),
          i,
          r.router
        )
    }.mkString("\n") +
      """|
         |def documentation = List(%s).foldLeft(List.empty[(String,String,String)]) { (s,e) => e match {
         |  case r @ (_,_,_) => s :+ r.asInstanceOf[(String,String,String)]
         |  case l => s ++ l.asInstanceOf[List[(String,String,String)]] 
         |}}
      """.stripMargin.format(
        rules.map {
          case Route(verb, path, call, _) if path.parts.isEmpty => "(\"\"\"" + verb + "\"\"\", prefix,\"\"\"" + call + "\"\"\")"
          case Route(verb, path, call, _) => "(\"\"\"" + verb + "\"\"\", prefix + (if(prefix.endsWith(\"/\")) \"\" else \"/\") + \"\"\"" + path + "\"\"\",\"\"\"" + call + "\"\"\")"
          case Include(prefix, router) => router + ".documentation"
        }.mkString(","))
  }

  /**
   * Generate the routing stuff
   */
  def routing(routes: List[Rule]): String = {
    Option(routes.zipWithIndex.map {
      case (r @ Include(_, _), i) =>
        """
            |%s
            |case %s%s(handler) => handler
        """.stripMargin.format(
          markLines(r),
          r.router.replace(".", "_"),
          i
        )
      case (r @ Route(_, _, _, _), i) =>
        """
            |%s
            |case %s%s(params) => {
            |   call%s { %s
            |        invokeHandler(%s%s, %s)
            |   }
            |}
        """.stripMargin.format(
          markLines(r),

          // alias
          r.call.packageName.replace(".", "_") + "_" + r.call.controller.replace(".", "_") + "_" + r.call.method,
          i,

          // binding
          r.call.parameters.filterNot(_.isEmpty).map { params =>
            params.map { p =>
              p.fixed.map { v =>
                """Param[""" + p.typeName + """]("""" + p.name + """", Right(""" + v + """))"""
              }.getOrElse {
                """params.""" + (if (r.path.has(p.name)) "fromPath" else "fromQuery") + """[""" + p.typeName + """]("""" + p.name + """", """ + p.default.map("Some(" + _ + ")").getOrElse("None") + """)"""
              }
            }.mkString(", ")
          }.map("(" + _ + ")").getOrElse(""),

          // local names
          r.call.parameters.filterNot(_.isEmpty).map { params =>
            params.map(_.name).mkString(", ")
          }.map("(" + _ + ") =>").getOrElse(""),

          // call
          if (r.call.instantiate) {
            "play.api.Play.maybeApplication.map(_.global).getOrElse(play.api.DefaultGlobal).getControllerInstance(classOf[" + r.call.packageName + "." + r.call.controller + "])." + r.call.method
          } else {
            r.call.packageName + "." + r.call.controller + "." + r.call.method
          },

          // call parameters
          r.call.parameters.map { params =>
            params.map(_.name).mkString(", ")
          }.map("(" + _ + ")").getOrElse(""),

          // definition
          """HandlerDef(this, """" + r.call.packageName + "." + r.call.controller + """", """" + r.call.method + """", """ + r.call.parameters.filterNot(_.isEmpty).map { params =>
            params.map("classOf[" + _.typeName + "]").mkString(", ")
          }.map("Seq(" + _ + ")").getOrElse("Nil") + ""","""" + r.verb + """", """ +"\"\"\""+ r.comments.map(_.comment).mkString("\n")+"\"\"\""+ """ , Routes.prefix + """" + r.path + """")""")
    }.mkString("\n")).filterNot(_.isEmpty).getOrElse {

      """Map.empty""" // Empty partial function

    }
  }


}

