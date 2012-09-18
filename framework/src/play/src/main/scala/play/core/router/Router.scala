package play.core

import play.api.mvc._
import play.api.mvc.Results._

/**
 * provides Play's router implementation
 */
object Router {

  import scala.util.parsing.input._
  import scala.util.parsing.combinator._
  import scala.util.matching._

  /**
   * captures URL parts
   */
  trait PathPart
  case class DynamicPart(name: String, constraint: String) extends PathPart with Positional {
    override def toString = """DynamicPart("""" + name + "\", \"\"\"" + constraint + "\"\"\")"
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

    def has(key: String): Boolean = parts.exists {
      case DynamicPart(name, _) if name == key => true
      case _ => false
    }

    override def toString = parts.map {
      case DynamicPart(name, constraint) => "$" + name + "<" + constraint + ">"
      case StaticPart(path) => path
    }.mkString

  }

  /**
   * provides a compiler for routes
   */
  object RoutesCompiler {

    object Hash {

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
        val routesContent = routeFile.slurpString

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

    /**
     * Generate the routing stuff
     */
    def routing(routes: List[Route]): String = {
      Option(routes.zipWithIndex.map {
        case (r, i) =>
          """
                        |%s
                        |case %s%s(params) => {
                        |   call%s { %s
                        |        invokeHandler(_root_.%s%s, %s)
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
            r.call.packageName + "." + r.call.controller + "." + r.call.field.map(_ + ".").getOrElse("") + r.call.method,

            // call parameters
            r.call.parameters.map { params =>
              params.map(_.name).mkString(", ")
            }.map("(" + _ + ")").getOrElse(""),

            // definition
            """HandlerDef(this, """" + r.call.packageName + "." + r.call.controller + r.call.field.map("." + _).getOrElse("") + """", """" + r.call.method + """", """ + r.call.parameters.filterNot(_.isEmpty).map { params =>
              params.map("classOf[" + _.typeName + "]").mkString(", ")
            }.map("Seq(" + _ + ")").getOrElse("Nil") + """)""")
      }.mkString("\n")).filterNot(_.isEmpty).getOrElse {

        """Map.empty""" // Empty partial function

      }
    }

    // --- Parser

    case class HttpVerb(value: String) {
      override def toString = value
    }
    case class HandlerCall(packageName: String, controller: String, method: String, field: Option[String], parameters: Option[Seq[Parameter]]) extends Positional {
      override def toString = packageName + "." + controller + "." + field.map(_ + ".").getOrElse("") + method + parameters.map { params =>
        "(" + params.mkString(", ") + ")"
      }.getOrElse("")
    }
    case class Parameter(name: String, typeName: String, fixed: Option[String], default: Option[String]) extends Positional {
      override def toString = name + ":" + typeName + fixed.map(" = " + _).getOrElse("") + default.map(" ?= " + _).getOrElse("")
    }
    case class Route(verb: HttpVerb, path: PathPattern, call: HandlerCall) extends Positional
    case class Comment(comment: String)

    class RouteFileParser extends JavaTokenParsers {

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

      def identifier: Parser[String] = namedError(ident, "Identifier expected")

      def end: util.matching.Regex = """\s*""".r

      def comment: Parser[Comment] = "#" <~ ".*".r ^^ {
        case c => Comment(c)
      }

      def newLine: Parser[String] = namedError((("\r"?) ~> "\n"), "End of line expected")

      def blankLine: Parser[Comment] = ignoreWhiteSpace ~> newLine ^^ { case _ => Comment("") }

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

      def httpVerb: Parser[HttpVerb] = namedError("GET" | "POST" | "PUT" | "HEAD" | "DELETE" | "OPTIONS", "HTTP Verb expected") ^^ {
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

      def path: Parser[PathPattern] = ((positioned(singleComponentPathPart) | positioned(multipleComponentsPathPart) | positioned(regexComponentPathPart) | staticPathPart) +) ^^ {
        case parts => PathPattern(parts)
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

      def call: Parser[HandlerCall] = namedError(rep1sep(identifier, "."), "Action call expected") ~ opt(parameters) ^^ {
        case handler ~ parameters =>
          {
            val packageName = handler.takeWhile(p => p.charAt(0).toUpper != p.charAt(0)).mkString(".")
            val className = try { handler(packageName.split('.').size) } catch { case _ => "" }
            val rest = handler.drop(packageName.split('.').size + 1)
            val field = Option(rest.dropRight(1).mkString(".")).filterNot(_.isEmpty)
            val methodName = rest.takeRight(1).mkString
            HandlerCall(packageName, className, methodName, field, parameters)
          }
      }

      def route = httpVerb ~ separator ~ path ~ separator ~ positioned(call) ~ ignoreWhiteSpace ^^ {
        case v ~ _ ~ p ~ _ ~ c ~ _ => Route(v, p, c)
      }

      def sentence: Parser[Product with Serializable] = (comment | positioned(route)) <~ (newLine | EOF)

      def parser: Parser[List[Route]] = phrase((blankLine | sentence *) <~ end) ^^ {
        case routes => routes.collect {
          case r @ Route(_, _, _) => r
        }
      }

      def parse(text: String): ParseResult[List[Route]] = {
        parser(new CharSequenceReader(text))
      }
    }

  }

  object Route {

    trait ParamsExtractor {
      def unapply(request: RequestHeader): Option[RouteParams]
    }

    def apply(method: String, pathPattern: PathPattern) = new ParamsExtractor{

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

  case class HandlerDef(ref: AnyRef, controller: String, method: String, parameterTypes: Seq[Class[_]]) {

    def getControllerClass: Class[_] = {
      Option(controller.split('.').takeRight(1).head).filter(p => p.charAt(0).toUpper != p.charAt(0)).map { field =>
        val parent = ref.getClass.getClassLoader.loadClass(controller.split('.').dropRight(1).mkString("."))
        try {
          parent.getMethod(field).getReturnType
        } catch {
          case _ => parent.getField(field).getType
        }
      }.getOrElse {
        ref.getClass.getClassLoader.loadClass(controller)
      }
    }

  }

  def queryString(items: List[Option[String]]) = {
    Option(items.filter(_.isDefined).map(_.get)).filterNot(_.isEmpty).map("?" + _.mkString("&")).getOrElse("")
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
          def controller = handler.getControllerClass
          def method = controller.getDeclaredMethod(handler.method, handler.parameterTypes.map {
            case c if c == classOf[Long] => classOf[java.lang.Long]
            case c => c
          }: _*)
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

    def documentation: Seq[(String, String, String)]

    def routes: PartialFunction[RequestHeader, Handler]

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

    def invokeHandler[T](call: => T, handler: HandlerDef)(implicit d: HandlerInvoker[T]): Handler = {
      d.call(call, handler)
    }

  }

}
