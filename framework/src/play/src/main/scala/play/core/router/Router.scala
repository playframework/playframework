package play.core

import play.api.mvc._
import play.api.mvc.Results._
import org.apache.commons.lang3.reflect.MethodUtils

import scala.util.parsing.input._
import scala.util.parsing.combinator._
import scala.util.matching._

<<<<<<< .merge_file_JZQjva
trait PathPart
=======
      def apply(bytes: Array[Byte]): String = {
        import java.security.MessageDigest
        val digest = MessageDigest.getInstance("SHA-1")
        digest.reset()
        digest.update(bytes)
        digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
      }

    }

    import scalax.file._
    import java.io.File

    case class RoutesCompilationError(source: File, message: String, line: Option[Int], column: Option[Int]) extends RuntimeException(message)

    case class GeneratedSource(file: File) {

      val lines = if (file.exists) Path(file).slurpString.split('\n').toList else Nil
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
>>>>>>> .merge_file_4q1BH4

case class DynamicPart(name: String, constraint: String) extends PathPart {
  override def toString = """DynamicPart("""" + name + "\", \"\"\"" + constraint + "\"\"\")" // "
}

case class StaticPart(value: String) extends PathPart {
  override def toString = """StaticPart("""" + value + """")"""
}

case class PathPattern(parts: Seq[PathPart]) {

  import java.util.regex._

  lazy val (regex, groups) = {
    Some(parts.foldLeft("", Map.empty[String, Int], 0) { (s, e) =>
      e match {
        case StaticPart(p) => ((s._1 + Pattern.quote(p)), s._2, s._3)
        case DynamicPart(k, r) => {
          ((s._1 + "(" + r + ")"), (s._2 + (k -> (s._3 + 1))), s._3 + 1 + Pattern.compile(r).matcher("").groupCount)
        }
      }
    }).map {
      case (r, g, _) => Pattern.compile("^" + r + "$") -> g
    }.get
  }

  def apply(path: String): Option[Map[String, String]] = {
    val matcher = regex.matcher(path)
    if (matcher.matches) {
      Some(groups.map {
        case (name, g) => name -> matcher.group(g)
      }.toMap)
    } else {
      None
    }
  }

<<<<<<< .merge_file_JZQjva
  def has(key: String): Boolean = parts.exists {
    case DynamicPart(name, _) if name == key => true
    case _ => false
  }
=======
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
                  "Cannot define fixed or default value for path extracted parameter " + name,
                  Some(p.pos.line),
                  Some(p.pos.column))
              }
              try {
                java.util.regex.Pattern.compile(regex)
              } catch {
                case e => {
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

    private def markLines(routes: Route*): String = {
      routes.map("// @LINE:" + _.pos.line).reverse.mkString("\n")
    }

    /**
     * Generate the actual Scala code for this router
     */
    private def generate(file: Path, routes: List[Route], additionalImports: Seq[String]): Seq[(String, String)] = {

      check(new File(file.path), routes);

      val (path, hash, date) = (file.path.replace(File.separator, "/"), Hash(file.byteArray), new java.util.Date().toString)

      Seq(("routes_reverseRouting.scala",
        """ |// @SOURCE:%s
                    |// @HASH:%s
                    |// @DATE:%s
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
                    |%s
                    |
                    |%s
                    |
                    |%s
                """.stripMargin.format(path, hash, date, additionalImports.map("import " + _).mkString("\n"), reverseRouting(routes), javaScriptReverseRouting(routes), refReverseRouting(routes))),
        ("routes_routing.scala",
          """ |// @SOURCE:%s
                    |// @HASH:%s
                    |// @DATE:%s
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
                    |%s 
                    |    
                    |def routes:PartialFunction[RequestHeader,Handler] = {        
                    |%s
                    |}
                    |    
                    |}
                """.stripMargin.format(path, hash, date, additionalImports.map("import " + _).mkString("\n"), routeDefinitions(routes), routing(routes)))) ++ {

          // Generate Java wrappers

          routes.groupBy(_.call.packageName).map {
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
                      val fields = routes.groupBy(_.call.field)
                      if (fields.size == 1 && fields.keys.head == None) {
                        "public static final " + packageName + ".Reverse" + controller + " " + controller + " = new " + packageName + ".Reverse" + controller + "();"
                      } else {
                        "public static class Reverse" + controller + " extends " + packageName + ".Reverse" + controller + " {\n" + {
                          fields.keys.collect { case Some(f) => f }.map { field =>
                            "public Reverse" + controller + "_" + field + " " + field + " = this.new Reverse" + controller + "_" + field + "();"
                          }.mkString("\n")
                        } + "\n}\n" +
                          "public static final Reverse" + controller + " " + controller + " = new Reverse" + controller + "();"
                      }
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

                  // group by field
                  routes.groupBy(_.call.field).map {
                    case (field, routes) => {

                      """
                        |%s
                        |%s
                        |%s
                      """.stripMargin.format(

                        field.map(f => "class Reverse" + controller.replace(".", "_") + "_" + f + " {").getOrElse(""),

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
                              route.path.parts.map {
                                case StaticPart(part) => "\"" + part + "\""
                                case DynamicPart(name, _) => {
                                  route.call.parameters.getOrElse(Nil).find(_.name == name).map { param =>
                                    "(\"\"\" + implicitly[PathBindable[" + param.typeName + "]].javascriptUnbind + \"\"\")" + """("""" + param.name + """", """ + localNames.get(param.name).getOrElse(param.name) + """)"""
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
                                                            |def %s = JavascriptReverseRoute(
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
                                  packageName + "." + controller + "." + route.call.field.map(_ + ".").getOrElse("") + route.call.method,
                                  "\"\"\"",
                                  reverseParameters.map(_._1.name).mkString(","),
                                  genCall(route),
                                  "\"\"\"")
                              }

                              case Seq(route, routes @ _*) => {
                                """ 
                                                            |%s
                                                            |def %s = JavascriptReverseRoute(
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

                        }.mkString("\n"),

                        field.map(_ => "}").getOrElse(""))

                    }
                  }.mkString("\n"))
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

                  // group by field
                  routes.groupBy(_.call.field).map {
                    case (field, routes) => {

                      """
                        |%s
                        |%s
                        |%s
                      """.stripMargin.format(
                        field.map(f => "class Reverse" + controller.replace(".", "_") + "_" + f + " {").getOrElse(""),

                        // reverse method
                        routes.groupBy(r => (r.call.method, r.call.parameters.getOrElse(Nil).map(p => p.typeName))).map {
                          case ((m, _), routes) =>

                            assert(routes.size > 0, "Empty routes set???")

                            val route = routes(0)

                            val parameters = route.call.parameters.getOrElse(Nil)

                            val reverseSignature = parameters.map(p => p.name + ":" + p.typeName).mkString(", ")

                            """ 
                                  |%s
                                  |def %s(%s) = new play.api.mvc.HandlerRef(
                                  |   %s, HandlerDef(this, "%s", "%s", %s)
                                  |)
                              """.stripMargin.format(
                              markLines(route),
                              route.call.method,
                              reverseSignature,
                              packageName + "." + controller + "." + route.call.field.map(_ + ".").getOrElse("") + route.call.method + "(" + { parameters.map(_.name).mkString(", ") } + ")",
                              packageName + "." + controller + route.call.field.map(_ + ".").getOrElse(""),
                              route.call.method,
                              "Seq(" + { parameters.map("classOf[" + _.typeName + "]").mkString(", ") } + ")")

                        }.mkString("\n"),

                        field.map(_ => "}").getOrElse(""))

                    }

                  }.mkString("\n"))
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

                  // group by field
                  routes.groupBy(_.call.field).map {
                    case (field, routes) => {

                      """
                        |%s
                        |%s
                        |%s
                      """.stripMargin.format(
                        field.map(f => "class Reverse" + controller.replace(".", "_") + "_" + f + " {").getOrElse(""),

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
                              route.path.parts.map {
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
                                                            |def %s(%s) = {
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
                                                            |def %s(%s) = {
                                                            |   (%s) match {
                                                            |%s    
                                                            |   }
                                                            |}
                                                        """.stripMargin.format(
                                  markLines((route +: routes): _*),
                                  route.call.method,
                                  reverseSignature,
                                  reverseParameters.map(_._1.name).mkString(", "),

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

                        }.mkString("\n"),

                        field.map(_ => "}").getOrElse(""))

                    }

                  }.mkString("\n"))
            }.mkString("\n"))

        }
      }.mkString("\n")

    }

    /**
     * Generate the routes definitions
     */
    def routeDefinitions(routes: List[Route]): String = {
      routes.zipWithIndex.map {
        case (r, i) =>
          """
                        |%s
                        |val %s%s = Route("%s", %s)
                    """.stripMargin.format(
            markLines(r),
            r.call.packageName.replace(".", "_") + "_" + r.call.controller.replace(".", "_") + "_" + r.call.method,
            i,
            r.verb.value,
            "PathPattern(List(" + r.path.parts.map(_.toString).mkString(",") + "))")
      }.mkString("\n") +
        """|
               |def documentation = List(%s)
            """.stripMargin.format(
          routes.map { r => "(\"\"\"" + r.verb + "\"\"\",\"\"\"" + r.path + "\"\"\",\"\"\"" + r.call + "\"\"\")" }.mkString(","))
    }
>>>>>>> .merge_file_4q1BH4

  override def toString = parts.map {
    case DynamicPart(name, constraint) => "$" + name + "<" + constraint + ">"
    case StaticPart(path) => path
  }.mkString

}

/**
 * provides Play's router implementation
 */
object Router {
  
   object Route {

    trait ParamsExtractor {
      def unapply(request: RequestHeader): Option[RouteParams]
    }
    def apply(method: String, pathPattern: PathPattern) = new ParamsExtractor {

      def unapply(request: RequestHeader): Option[RouteParams] = {
        if (method == request.method) {
          pathPattern(request.path).map { groups =>
            RouteParams(groups, request.queryString)
          }
        } else {
          None
        }
      }

    }

  }

  object Include {

<<<<<<< .merge_file_JZQjva
    def apply(router: Router.Routes) = new {
=======
    trait ParamsExtractor {
      def unapply(request: RequestHeader): Option[RouteParams]
    }

    def apply(method: String, pathPattern: PathPattern) = new ParamsExtractor{
>>>>>>> .merge_file_4q1BH4

      def unapply(request: RequestHeader): Option[Handler] = {
        router.routes.lift(request)
      }

    }

  }

  case class JavascriptReverseRoute(name: String, f: String)

  case class Param[T](name: String, value: Either[String, T])

  case class RouteParams(path: Map[String, String], queryString: Map[String, Seq[String]]) {

    def fromPath[T](key: String, default: Option[T] = None)(implicit binder: PathBindable[T]): Param[T] = {
      Param(key, path.get(key).map(binder.bind(key, _)).getOrElse {
        default.map(d => Right(d)).getOrElse(Left("Missing parameter: " + key))
      })
    }

    def fromQuery[T](key: String, default: Option[T] = None)(implicit binder: QueryStringBindable[T]): Param[T] = {
      Param(key, binder.bind(key, queryString).getOrElse {
        default.map(d => Right(d)).getOrElse(Left("Missing parameter: " + key))
      })
    }

  }

  case class HandlerDef(ref: AnyRef, controller: String, method: String, parameterTypes: Seq[Class[_]], verb: String, comments: String, path: String)

  def queryString(items: List[Option[String]]) = {
    Option(items.filter(_.isDefined).map(_.get).filterNot(_.isEmpty)).filterNot(_.isEmpty).map("?" + _.mkString("&")).getOrElse("")
  }

  // HandlerInvoker

  @scala.annotation.implicitNotFound("Cannot use a method returning ${T} as an Handler")
  trait HandlerInvoker[T] {
    def call(call: => T, handler: HandlerDef): Handler
  }

  object HandlerInvoker {

    implicit def passThrough[A <: Handler]: HandlerInvoker[A] = new HandlerInvoker[A] {
      def call(call: => A, handler: HandlerDef): Handler = call
    }

    implicit def wrapJava: HandlerInvoker[play.mvc.Result] = new HandlerInvoker[play.mvc.Result] {
      def call(call: => play.mvc.Result, handler: HandlerDef) = {
        new play.core.j.JavaAction {
          def invocation = call
          lazy val controller = handler.ref.getClass.getClassLoader.loadClass(handler.controller)
          lazy val method = MethodUtils.getMatchingAccessibleMethod(controller, handler.method, handler.parameterTypes: _*)
        }
      }
    }

    implicit def javaBytesWebSocket: HandlerInvoker[play.mvc.WebSocket[Array[Byte]]] = new HandlerInvoker[play.mvc.WebSocket[Array[Byte]]] {
      def call(call: => play.mvc.WebSocket[Array[Byte]], handler: HandlerDef): Handler = play.core.j.JavaWebSocket.ofBytes(call)
    }

    implicit def javaStringWebSocket: HandlerInvoker[play.mvc.WebSocket[String]] = new HandlerInvoker[play.mvc.WebSocket[String]] {
      def call(call: => play.mvc.WebSocket[String], handler: HandlerDef): Handler = play.core.j.JavaWebSocket.ofString(call)
    }

    implicit def javaJsonWebSocket: HandlerInvoker[play.mvc.WebSocket[org.codehaus.jackson.JsonNode]] = new HandlerInvoker[play.mvc.WebSocket[org.codehaus.jackson.JsonNode]] {
      def call(call: => play.mvc.WebSocket[org.codehaus.jackson.JsonNode], handler: HandlerDef): Handler = play.core.j.JavaWebSocket.ofJson(call)
    }

  }

  trait Routes {
    self =>

    def documentation: Seq[(String, String, String)]

    def routes: PartialFunction[RequestHeader, Handler]

    def setPrefix(prefix: String)

    def prefix: String

    //

    def badRequest(error: String) = Action { request =>
      play.api.Play.maybeApplication.map(_.global.onBadRequest(request, error)).getOrElse(play.api.DefaultGlobal.onBadRequest(request, error))
    }

    def call(generator: => Handler): Handler = {
      generator
    }

    def call[P](pa: Param[P])(generator: (P) => Handler): Handler = {
      pa.value.fold(badRequest, generator)
    }

    def call[A1, A2](pa1: Param[A1], pa2: Param[A2])(generator: Function2[A1, A2, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right)
        yield (a1, a2))
        .fold(badRequest, { case (a1, a2) => generator(a1, a2) })
    }

    def call[A1, A2, A3](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3])(generator: Function3[A1, A2, A3, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right)
        yield (a1, a2, a3))
        .fold(badRequest, { case (a1, a2, a3) => generator(a1, a2, a3) })
    }

    def call[A1, A2, A3, A4](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4])(generator: Function4[A1, A2, A3, A4, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right)
        yield (a1, a2, a3, a4))
        .fold(badRequest, { case (a1, a2, a3, a4) => generator(a1, a2, a3, a4) })
    }

    def call[A1, A2, A3, A4, A5](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5])(generator: Function5[A1, A2, A3, A4, A5, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right)
        yield (a1, a2, a3, a4, a5))
        .fold(badRequest, { case (a1, a2, a3, a4, a5) => generator(a1, a2, a3, a4, a5) })
    }

    def call[A1, A2, A3, A4, A5, A6](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6])(generator: Function6[A1, A2, A3, A4, A5, A6, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right)
        yield (a1, a2, a3, a4, a5, a6))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6) => generator(a1, a2, a3, a4, a5, a6) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7])(generator: Function7[A1, A2, A3, A4, A5, A6, A7, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7) => generator(a1, a2, a3, a4, a5, a6, a7) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8])(generator: Function8[A1, A2, A3, A4, A5, A6, A7, A8, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8) => generator(a1, a2, a3, a4, a5, a6, a7, a8) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9])(generator: Function9[A1, A2, A3, A4, A5, A6, A7, A8, A9, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10])(generator: Function10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11])(generator: Function11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12])(generator: Function12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13])(generator: Function13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14])(generator: Function14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15])(generator: Function15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right; a15 <- pa15.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16])(generator: Function16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right; a15 <- pa15.value.right; a16 <- pa16.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17])(generator: Function17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right; a15 <- pa15.value.right; a16 <- pa16.value.right; a17 <- pa17.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18])(generator: Function18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right; a15 <- pa15.value.right; a16 <- pa16.value.right; a17 <- pa17.value.right; a18 <- pa18.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18], pa19: Param[A19])(generator: Function19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right; a15 <- pa15.value.right; a16 <- pa16.value.right; a17 <- pa17.value.right; a18 <- pa18.value.right; a19 <- pa19.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18], pa19: Param[A19], pa20: Param[A20])(generator: Function20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right; a15 <- pa15.value.right; a16 <- pa16.value.right; a17 <- pa17.value.right; a18 <- pa18.value.right; a19 <- pa19.value.right; a20 <- pa20.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20) })
    }

    def call[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21](pa1: Param[A1], pa2: Param[A2], pa3: Param[A3], pa4: Param[A4], pa5: Param[A5], pa6: Param[A6], pa7: Param[A7], pa8: Param[A8], pa9: Param[A9], pa10: Param[A10], pa11: Param[A11], pa12: Param[A12], pa13: Param[A13], pa14: Param[A14], pa15: Param[A15], pa16: Param[A16], pa17: Param[A17], pa18: Param[A18], pa19: Param[A19], pa20: Param[A20], pa21: Param[A21])(generator: Function21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, Handler]): Handler = {
      (for (a1 <- pa1.value.right; a2 <- pa2.value.right; a3 <- pa3.value.right; a4 <- pa4.value.right; a5 <- pa5.value.right; a6 <- pa6.value.right; a7 <- pa7.value.right; a8 <- pa8.value.right; a9 <- pa9.value.right; a10 <- pa10.value.right; a11 <- pa11.value.right; a12 <- pa12.value.right; a13 <- pa13.value.right; a14 <- pa14.value.right; a15 <- pa15.value.right; a16 <- pa16.value.right; a17 <- pa17.value.right; a18 <- pa18.value.right; a19 <- pa19.value.right; a20 <- pa20.value.right; a21 <- pa21.value.right)
        yield (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21))
        .fold(badRequest, { case (a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) => generator(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21) })
    }

    def handlerFor(request: RequestHeader): Option[Handler] = {
      routes.lift(request)
    }

    private def tagRequest(rh: RequestHeader, handler: HandlerDef): RequestHeader = rh.copy(tags = rh.tags ++ Map(
      play.api.Routes.ROUTE_PATTERN -> handler.path,
      play.api.Routes.ROUTE_VERB -> handler.verb,
      play.api.Routes.ROUTE_CONTROLLER -> handler.controller,
      play.api.Routes.ROUTE_ACTION_METHOD -> handler.method,
      play.api.Routes.ROUTE_COMMENTS -> handler.comments
    ))

    def invokeHandler[T](call: => T, handler: HandlerDef)(implicit d: HandlerInvoker[T]): Handler = {
      d.call(call, handler) match {
        case javaAction: play.core.j.JavaAction => new play.core.j.JavaAction {
          def invocation = javaAction.invocation
          def controller = javaAction.controller
          def method = javaAction.method
          override def apply(req: Request[play.mvc.Http.RequestBody]): Result = {
            javaAction(Request(tagRequest(req, handler), req.body))
          }
        }
        case action: EssentialAction => new EssentialAction {
          def apply(rh: RequestHeader) = action(tagRequest(rh, handler))
        }
        case ws @ WebSocket(f) => {
          WebSocket[ws.FRAMES_TYPE](rh => f(tagRequest(rh, handler)))(ws.frameFormatter)
        }
        case handler => handler
      }
    }

  }

}
