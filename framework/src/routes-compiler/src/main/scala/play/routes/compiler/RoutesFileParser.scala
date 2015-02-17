/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.routes.compiler

import java.io.File

import org.apache.commons.io.FileUtils

import scala.util.parsing.combinator._
import scala.util.parsing.input._

import scala.language.postfixOps

object RoutesFileParser {

  /**
   * Parse the given routes file
   *
   * @param routesFile The routes file to parse
   * @return Either the list of compilation errors encountered, or a list of routing rules
   */
  def parse(routesFile: File): Either[Seq[RoutesCompilationError], List[Rule]] = {

    val routesContent = FileUtils.readFileToString(routesFile)

    parseContent(routesContent, routesFile)
  }

  /**
   * Parse the given routes file content
   *
   * @param routesContent The content of the routes file
   * @param routesFile The routes file (used for error reporting)
   * @return Either the list of compilation errors encountered, or a list of routing rules
   */
  def parseContent(routesContent: String, routesFile: File): Either[Seq[RoutesCompilationError], List[Rule]] = {
    val parser = new RoutesFileParser()

    parser.parse(routesContent) match {
      case parser.Success(parsed: List[Rule], _) =>
        validate(routesFile, parsed.collect { case r: Route => r }) match {
          case Nil => Right(parsed)
          case errors => Left(errors)
        }
      case parser.NoSuccess(message, in) =>
        Left(Seq(RoutesCompilationError(routesFile, message, Some(in.pos.line), Some(in.pos.column))))
    }
  }

  /**
   * Validate the routes file
   */
  private def validate(file: java.io.File, routes: List[Route]): Seq[RoutesCompilationError] = {

    import scala.collection.mutable._
    val errors = ListBuffer.empty[RoutesCompilationError]

    routes.foreach { route =>

      if (route.call.packageName.isEmpty) {
        errors += RoutesCompilationError(
          file,
          "Missing package name",
          Some(route.call.pos.line),
          Some(route.call.pos.column))
      }

      if (route.call.controller.isEmpty) {
        errors += RoutesCompilationError(
          file,
          "Missing Controller",
          Some(route.call.pos.line),
          Some(route.call.pos.column))
      }

      route.path.parts.collect {
        case part @ DynamicPart(name, regex, _) => {
          route.call.parameters.getOrElse(Nil).find(_.name == name).map { p =>
            if (p.fixed.isDefined || p.default.isDefined) {
              errors += RoutesCompilationError(
                file,
                "It is not allowed to specify a fixed or default value for parameter: '" + name + "' extracted from the path",
                Some(p.pos.line),
                Some(p.pos.column))
            }
            try {
              java.util.regex.Pattern.compile(regex)
            } catch {
              case e: Exception => {
                errors += RoutesCompilationError(
                  file,
                  e.getMessage,
                  Some(part.pos.line),
                  Some(part.pos.column))
              }
            }
          }.getOrElse {
            errors += RoutesCompilationError(
              file,
              "Missing parameter in call definition: " + name,
              Some(part.pos.line),
              Some(part.pos.column))
          }
        }
      }

    }

    // make sure there are no routes using overloaded handler methods, or handler methods with default parameters without declaring them all
    val sameHandlerMethodGroup = routes.groupBy { r =>
      r.call.packageName + r.call.controller + r.call.method
    }

    val sameHandlerMethodParameterCountGroup = sameHandlerMethodGroup.groupBy { g =>
      (g._1, g._2.groupBy(route => route.call.parameters.map(p => p.length).getOrElse(0)))
    }

    sameHandlerMethodParameterCountGroup.find(g => g._1._2.size > 1).foreach { overloadedRouteGroup =>
      val firstOverloadedRoute = overloadedRouteGroup._2.values.head.head
      errors += RoutesCompilationError(
        file,
        "Using different overloaded methods is not allowed. If you are using a single method in combination with default parameters, make sure you declare them all explicitly.",
        Some(firstOverloadedRoute.call.pos.line),
        Some(firstOverloadedRoute.call.pos.column)
      )

    }

    errors.toList
  }

}

/**
 * The routes file parser
 */
private[routes] class RoutesFileParser extends JavaTokenParsers {

  override def skipWhitespace = false
  override val whiteSpace = """[ \t]+""".r

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
        case Success(x, rest) =>
          elems += x; applyp(rest)
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
    case name => DynamicPart(name, """[^/]+""", encode = true)
  }

  def multipleComponentsPathPart: Parser[DynamicPart] = ("*" ~> identifier) ^^ {
    case name => DynamicPart(name, """.+""", encode = false)
  }

  def regexComponentPathPart: Parser[DynamicPart] = "$" ~> identifier ~ ("<" ~> (not(">") ~> """[^\s]""".r +) <~ ">" ^^ { case c => c.mkString }) ^^ {
    case name ~ regex => DynamicPart(name, regex, encode = false)
  }

  def staticPathPart: Parser[StaticPart] = (not(":") ~> not("*") ~> not("$") ~> """[^\s]""".r +) ^^ {
    case chars => StaticPart(chars.mkString)
  }

  def path: Parser[PathPattern] = "/" ~ ((positioned(singleComponentPathPart) | positioned(multipleComponentsPathPart) | positioned(regexComponentPathPart) | staticPathPart) *) ^^ {
    case _ ~ parts => PathPattern(parts)
  }

  def space(s: String): Parser[String] = (ignoreWhiteSpace ~> s <~ ignoreWhiteSpace)

  def parameterType: Parser[String] = ":" ~> ignoreWhiteSpace ~> simpleType

  def simpleType: Parser[String] = {
    ((stableId <~ ignoreWhiteSpace) ~ opt(typeArgs)) ^^ {
      case sid ~ ta => sid.toString + ta.getOrElse("")
    } |
      (space("(") ~ types ~ space(")")) ^^ {
        case _ ~ b ~ _ => "(" + b + ")"
      }
  }

  def typeArgs: Parser[String] = {
    (space("[") ~ types ~ space("]") ~ opt(typeArgs)) ^^ {
      case _ ~ ts ~ _ ~ ta => "[" + ts + "]" + ta.getOrElse("")
    } |
      (space("#") ~ identifier ~ opt(typeArgs)) ^^ {
        case _ ~ id ~ ta => "#" + id + ta.getOrElse("")
      }
  }

  def types: Parser[String] = rep1sep(simpleType, space(",")) ^^ (_ mkString ",")

  def stableId: Parser[String] = rep1sep(identifier, space(".")) ^^ (_ mkString ".")

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
      routes.reverse.foldLeft(List[(Option[Rule], List[Comment])]()) {
        case (s, r @ Route(_, _, _, _)) => (Some(r), List()) :: s
        case (s, i @ Include(_, _)) => (Some(i), List()) :: s
        case (s, c @ ()) => (None, List()) :: s
        case ((r, comments) :: others, c @ Comment(_)) => (r, c :: comments) :: others
        case (s, _) => s
      }.collect {
        case (Some(r @ Route(_, _, _, _)), comments) => r.copy(comments = comments).setPos(r.pos)
        case (Some(i @ Include(_, _)), _) => i
      }
  }

  def parse(text: String): ParseResult[List[Rule]] = {
    parser(new CharSequenceReader(text))
  }
}
