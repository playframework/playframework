package play.core

import play.api.mvc._
import play.api.mvc.Results._

object Router {
    
    import scala.util.parsing.input._
    import scala.util.parsing.combinator._
    import scala.util.matching._

    trait PathPart
    case class DynamicPart(name:String, constraint:String) extends PathPart with Positional {
        override def toString = """DynamicPart("""" + name + "\", \"\"\"" + constraint + "\"\"\")"
    }
    case class StaticPart(value:String) extends PathPart {
        override def toString = """StaticPart("""" + value + """")"""
    }
    case class PathPattern(parts:Seq[PathPart]) {
        
        import java.util.regex._
        
        lazy val (regex,groups) = {
            Some(parts.foldLeft("", Map.empty[String,Int], 0) { (s,e) => 
                e match {
                    case StaticPart(p) => ((s._1 + Pattern.quote(p)), s._2, s._3)
                    case DynamicPart(k, r) => {
                        ((s._1 + "(" + r + ")"), (s._2 + (k -> (s._3 + 1))), s._3 + 1 + Pattern.compile(r).matcher("").groupCount)
                    }
                } 
            }).map {
                case (r,g,_) => Pattern.compile("^" + r + "$") -> g
            }.get
        }
        
        def apply(path:String) = {
            val matcher = regex.matcher(path)
            if(matcher.matches) {
                Some(groups.map {
                    case (name,g) => name -> matcher.group(g)
                }.toMap)
            } else {
                None
            }
        }
        
        def has(key:String) = parts.exists {
            case DynamicPart(name, _) if name == key => true
            case _ => false
        }
        
    }
    
    object RoutesCompiler {

        object Hash {

            def apply(bytes:Array[Byte]) = {
                import java.security.MessageDigest
                val digest = MessageDigest.getInstance("SHA-1")
                digest.reset()
                digest.update(bytes)
                digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
            }

        }

        import scalax.file._
        import java.io.File

        case class RoutesCompilationError(source:File, message:String, line:Option[Int], column:Option[Int]) extends RuntimeException(message)

        case class GeneratedSource(file:File) {

            val lines = if(file.exists) Path(file).slurpString.split('\n').toList else Nil
            val source = lines.headOption.filter(_.startsWith("// @SOURCE:")).map(m => Path(m.drop(11)))

            def isGenerated = source.isDefined

            def sync() = if(!source.get.exists) file.delete() else false

            def needsRecompilation = {
                val hash = lines.find(_.startsWith("// @HASH:")).map(m => m.drop(9)).getOrElse("")
                source.filter(_.exists).map { p =>
                    Hash(p.byteArray) != hash
                }.getOrElse(true)
            }

            def mapLine(generatedLine:Int) = {
                lines.take(generatedLine).reverse.collect {
                    case l if l.startsWith("// @LINE:") => Integer.parseInt(l.drop(9))
                }.headOption
            }

        }
        
        object MaybeGeneratedSource {
            
            def unapply(source:File) = {
                val generated = GeneratedSource(source)
                if(generated.isGenerated) {
                    Some(generated)
                } else {
                    None
                }
            }
            
        }

        def compile(file:File, generatedDir:File) {
            
            val generated = GeneratedSource(new File(generatedDir, "routes_main.scala"))
            
            if(generated.needsRecompilation) {
                
                val parser = new RouteFileParser
                val routeFile = Path(file).toAbsolute
                val routesContent = routeFile.slurpString
                
                Path(generated.file).write(
                    parser.parse(routesContent) match {
                        case parser.Success(parsed, _) => generate(routeFile, parsed)
                        case parser.NoSuccess(message, in) => {
                            throw RoutesCompilationError(file, message, Some(in.pos.line), Some(in.pos.column))
                        }
                    }
                )
            }
            
        }

        /**
         * Precheck routes coherence or throw exceptions early
         */
        private def check(file:java.io.File, routes:List[Route]) {
            routes.foreach { route =>

                if(route.call.packageName.isEmpty) {
                    throw RoutesCompilationError(
                        file,
                        "Missing package name",
                        Some(route.call.pos.line),
                        Some(route.call.pos.column)
                    )
                }

                route.path.parts.collect {
                    case part@DynamicPart(name, regex) => {
                        route.call.parameters.getOrElse(Nil).find(_.name == name).map { p =>
                            if(p.fixed.isDefined || p.default.isDefined) {
                                throw RoutesCompilationError(
                                    file,
                                    "Cannot define fixed or default value for path extracted parameter " + name,
                                    Some(p.pos.line),
                                    Some(p.pos.column)
                                )
                            }
                            try {
                                java.util.regex.Pattern.compile(regex)
                            } catch {
                                case e => {
                                    throw RoutesCompilationError(
                                        file,
                                        e.getMessage,
                                        Some(part.pos.line),
                                        Some(part.pos.column)
                                    )
                                }
                            }
                        }.getOrElse {
                            throw RoutesCompilationError(
                                file,
                                "Missing parameter in call definition: " + name,
                                Some(part.pos.line),
                                Some(part.pos.column)
                            )
                        }
                    }
                }

            }
        }

        private def markLines(routes:Route*) = {
            routes.map("// @LINE:" + _.pos.line).reverse.mkString("\n")
        }

        /**
         * Generate the actual Scala code for this router
         */
        private def generate(file:Path, routes:List[Route]) = {

            check(new File(file.path), routes);

            """ |// @SOURCE:%s
                |// @HASH:%s
                |// @DATE:%s
                |
                |import play.core._
                |import play.core.Router._
                |import play.core.j._
                |
                |import play.api.mvc._
                |
                |import Router.queryString
                |
                |%s
                |
                |object Routes extends Router.Routes {
                |
                |%s 
                |    
                |def routes:PartialFunction[Request,Action] = {
                |        
                |%s
                |        
                |}
                |    
                |}
            """.stripMargin.format(
                file.path,
                Hash(file.byteArray),
                new java.util.Date().toString,
                reverseRouting(routes), 
                routeDefinitions(routes), 
                routing(routes)
            )
        }

        /**
         * Generate the reverse routing operations
         */
        def reverseRouting(routes:List[Route]) = {

            routes.groupBy(_.call.packageName).map {
                case (packageName, routes) => {

                    """
                        |%s
                        |package %s {
                        |%s
                        |
                        |object routes {
                        |%s 
                        |}
                        |
                        |}
                    """.stripMargin.format(
                        markLines(routes:_*),
                        packageName,

                        routes.groupBy(_.call.controller).map { 
                            case (controller, routes) => 
                            """
                                |%s
                                |object routes_%s {
                                |    
                                |%s
                                |    
                                |}
                            """.stripMargin.format(
                                markLines(routes:_*),

                                // alias
                                controller.replace(".", "_"),

                                // reverse method
                                routes.groupBy(r => r.call.method -> r.call.parameters.getOrElse(Nil).map(p => p.typeName)).map { 
                                    case ((m, _), routes) =>

                                        assert(routes.size > 0, "Empty routes set???")

                                        val parameters = routes(0).call.parameters.getOrElse(Nil)

                                        val reverseParameters = parameters.zipWithIndex.filterNot {
                                            case (p,i) => {
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

                                        def genCall(route:Route, localNames:Map[String,String] = Map()) = """Call("%s", %s%s)""".format(
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

                                                if(queryParams.size == 0) {
                                                    ""
                                                } else {
                                                    """ + queryString(List(%s))""".format(
                                                        queryParams.map { p =>
                                                            ("""implicitly[QueryStringBindable[""" + p.typeName + """]].unbind("""" + p.name + """", """ + localNames.get(p.name).getOrElse(p.name) + """)""") -> p
                                                        }.map { 
                                                            case (u, Parameter(name, typeName, None, Some(default))) => """if(""" + localNames.get(name).getOrElse(name) + """ == """ + default + """) None else Some(""" + u + """)"""
                                                            case (u, Parameter(name, typeName, None, None)) => "Some(" + u + ")"
                                                        }.mkString(", ")
                                                    )

                                                }

                                            }

                                        )

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
                                                    genCall(route)
                                                )
                                            }

                                            case Seq(route, routes@_*) => {
                                                """ 
                                                    |%s
                                                    |def %s(%s) = {
                                                    |   (%s) match {
                                                    |%s    
                                                    |   }
                                                    |}
                                                """.stripMargin.format(
                                                    markLines((route +: routes):_*),
                                                    route.call.method,
                                                    reverseSignature,
                                                    reverseParameters.map(_._1.name).mkString(", "),

                                                    // route selection
                                                    (route +: routes).map { route =>

                                                        val localNames = reverseParameters.map {
                                                            case (lp,i) => route.call.parameters.get(i).name -> lp.name
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
                                                            }).filterNot(_.isEmpty).map("if " + _.mkString(" && ")).getOrElse(""),

                                                            genCall(route, localNames)
                                                        )

                                                    }.mkString("\n")
                                                )
                                            }

                                        }

                                }.mkString("\n")

                            )
                        }.mkString("\n"),

                        // routes object
                        routes.groupBy(_.call.controller).map { 
                            case (controller, _) => "def %s() = routes_%s".format(controller, controller)
                        }.mkString("\n")

                    )

                }
            }.mkString("\n")

        }

        /**
         * Generate the routes definitions
         */
        def routeDefinitions(routes:List[Route]) = {
            routes.zipWithIndex.map {
                case (r,i) => 
                    """
                        |%s
                        |val %s%s = Route("%s", %s)
                    """.stripMargin.format(
                            markLines(r),
                            r.call.packageName.replace(".", "_") + "_" + r.call.controller.replace(".", "_") + "_" + r.call.method,
                            i,
                            r.verb.value,
                            r.path
                      )
            }.mkString("\n")
        }

        /**
         * Generate the routing stuff
         */
        def routing(routes:List[Route]) = {
            routes.zipWithIndex.map {
                case (r,i) => 
                    """
                        |%s
                        |case %s%s(params) => {
                        |   call%s { %s
                        |        invokeAction(_root_.%s%s, %s)
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
                                    """params.""" + (if(r.path.has(p.name)) "fromPath" else "fromQuery" ) + """[""" + p.typeName + """]("""" + p.name + """", """ + p.default.map("Some(" + _ + ")").getOrElse("None") + """)"""
                                }
                            }.mkString(", ")
                        }.map("(" + _ + ")").getOrElse(""),

                        // local names
                        r.call.parameters.filterNot(_.isEmpty).map { params =>
                            params.map(_.name).mkString(", ")
                        }.map("(" + _ + ") =>").getOrElse(""),

                        // call
                        r.call.packageName + "." + r.call.controller + "." + r.call.method,

                        // call parameters
                        r.call.parameters.map { params =>
                            params.map(_.name).mkString(", ")
                        }.map("(" + _ + ")").getOrElse(""),

                        // definition
                        """ActionDef(this, """"+ r.call.packageName + "." + r.call.controller + """", """"+ r.call.method + """", """ + r.call.parameters.filterNot(_.isEmpty).map { params =>
                            params.map("classOf[" + _.typeName + "]").mkString(", ")
                        }.map("Seq(" + _ + ")").getOrElse("Nil") + """)"""

                    )
            }.mkString("\n")
        }

        // --- Parser

        case class HttpVerb(value:String)
        case class ActionCall(packageName:String, controller:String, method:String, parameters:Option[Seq[Parameter]]) extends Positional
        case class Parameter(name:String, typeName:String, fixed:Option[String], default:Option[String]) extends Positional
        case class Route(verb:HttpVerb, path:PathPattern, call:ActionCall) extends Positional
        case class Comment(comment:String)

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

            def namedError[A](p:Parser[A], msg:String) = Parser[A] { i =>
                p(i) match {
                    case Failure(_, in) => Failure(msg, in)
                    case o => o
                }
            }

            def several[T](p: => Parser[T]): Parser[List[T]] = Parser { in =>
                import scala.collection.mutable.ListBuffer
                val elems = new ListBuffer[T]
                def continue(in: Input): ParseResult[List[T]] = {
                    val p0 = p    // avoid repeatedly re-evaluating by-name parser
                    @scala.annotation.tailrec def applyp(in0: Input): ParseResult[List[T]] = p0(in0) match {
                        case Success(x, rest) => elems += x ; applyp(rest)
                        case Failure(_, _)    => Success(elems.toList, in0)
                        case err:Error        => err
                    }
                    applyp(in)
                }
                continue(in)
            }

            def separator = namedError(whiteSpace, "Whitespace expected")

            def ignoreWhiteSpace = opt(whiteSpace)

            def identifier = namedError(ident, "Identifier expected")

            def end = """\s*""".r

            def comment = "#" <~ ".*".r ^^ {
                case c => Comment(c)
            }

            def newLine = namedError("\n", "End of line expected")

            def blankLine = ignoreWhiteSpace ~> newLine ^^ {case _ => Comment("")}

            def parentheses:Parser[String] = {
                "(" ~ (several((parentheses | not(")") ~> """.""".r))) ~ commit(")") ^^ {
                    case p1~charList~p2 => p1 + charList.mkString + p2
                }
            }

            def brackets:Parser[String] = {
                "[" ~ (several((parentheses | not("]") ~> """.""".r))) ~ commit("]") ^^ {
                    case p1~charList~p2 => p1 + charList.mkString + p2
                }
            }

            def string:Parser[String] = {
                "\"" ~ (several((parentheses | not("\"") ~> """.""".r))) ~ commit("\"") ^^ {
                    case p1~charList~p2 => p1 + charList.mkString + p2
                }
            }

            def multiString:Parser[String] = {
                "\"\"\"" ~ (several((parentheses | not("\"\"\"") ~> """.""".r))) ~ commit("\"\"\"") ^^ {
                    case p1~charList~p2 => p1 + charList.mkString + p2
                }
            }

            def httpVerb = namedError("GET" | "POST" | "PUT" | "HEAD" | "DELETE", "HTTP Verb expected") ^^ {
                case v => HttpVerb(v)
            }

            def singleComponentPathPart = (":" ~> identifier) ^^ {
                case name => DynamicPart(name, """[^/]+""")
            }

            def multipleComponentsPathPart = ("*" ~> identifier) ^^ {
                case name => DynamicPart(name, """.+""")
            }

            def regexComponentPathPart = "$" ~> identifier ~ ("<" ~> (not(">") ~> """[^\s]""".r + ) <~ ">" ^^ {case c => c.mkString}) ^^ {
                case name~regex => DynamicPart(name, regex)
            }

            def staticPathPart = (not(":") ~> not("*") ~> not("$") ~> """[^\s]""".r +) ^^ {
                case chars => StaticPart(chars.mkString)
            } 

            def path = ( ( positioned(singleComponentPathPart) | positioned(multipleComponentsPathPart) | positioned(regexComponentPathPart) | staticPathPart ) + ) ^^ {
                case parts => PathPattern(parts)
            }

            def parameterType = ":" ~> ignoreWhiteSpace ~> rep1sep(identifier, ".") ~ opt(brackets) ^^ {
                case t~g => t.mkString(".") + g.getOrElse("")
            }

            def expression = (multiString | string | parentheses | brackets | """[^),?=\n]""".r +) ^^ {
                case p => p.mkString
            }

            def parameterFixedValue = "=" ~ ignoreWhiteSpace ~ expression ^^ {
                case a~_~b => a + b
            }

            def parameterDefaultValue = "?=" ~ ignoreWhiteSpace ~ expression ^^ {
                case a~_~b => a + b
            }

            def parameter = (identifier <~ ignoreWhiteSpace) ~ opt(parameterType) ~ (ignoreWhiteSpace ~> opt(parameterDefaultValue | parameterFixedValue)) ^^ {
                case name~t~d => Parameter(name, t.getOrElse("String"), d.filter(_.startsWith("=")).map(_.drop(1)), d.filter(_.startsWith("?")).map(_.drop(2)))
            }

            def parameters = "(" ~> repsep(ignoreWhiteSpace ~> positioned(parameter) <~ ignoreWhiteSpace, ",") <~ ")" 

            def call = namedError(rep1sep(identifier, "."), "Action call expected") ~ opt(parameters) ^^ {
                case action~parameters => ActionCall(action.dropRight(2).mkString("."), action.takeRight(2).dropRight(1).mkString("."), action.takeRight(1).mkString, parameters)
            }

            def route = httpVerb ~ separator ~ path ~ separator ~ positioned(call) ~ ignoreWhiteSpace ^^ {
                case v~_~p~_~c~_ => Route(v,p,c)
            }

            def sentence = (comment | positioned(route)) <~ newLine

            def parser:Parser[List[Route]] = phrase( ( blankLine | sentence *) <~ end ) ^^ {
                case routes => routes.collect {
                    case r@Route(_,_,_) => r
                }
            }

            def parse(text:String) = {
                parser(new CharSequenceReader(text))
            }

        }

    }
    
    object Route {
        
        def apply(method:String, pathPattern:PathPattern) = new {

            def unapply(request:Request):Option[RouteParams] = {
                if(method == request.method) {
                    pathPattern(request.path).map { groups =>
                        RouteParams(groups, request.queryString)
                    }
                } else  {
                    None
                }
            }

        }
        
    }
    
    case class Param[T](name:String, value:Either[String,T])
    
    case class RouteParams(path:Map[String,String], queryString:Map[String,Seq[String]]) {
        
        def fromPath[T](key:String, default:Option[T] = None)(implicit binder:PathBindable[T]):Param[T] = {
            Param(key, path.get(key).map(binder.bind(key, _)).getOrElse {
                default.map(d => Right(d)).getOrElse(Left("Missing parameter: " + key))
            })
        }
        
        def fromQuery[T](key:String, default:Option[T] = None)(implicit binder:QueryStringBindable[T]):Param[T] = {
            Param(key, binder.bind(key, queryString).getOrElse {
                default.map(d => Right(d)).getOrElse(Left("Missing parameter: " + key))
            })
        }
        
    }
    
    case class ActionDef(ref:AnyRef, controller:String, method:String, parameterTypes:Seq[Class[_]])
            
    def queryString(items:List[Option[String]]) = {
        Option(items.filter(_.isDefined).map(_.get)).filterNot(_.isEmpty).map("?" + _.mkString("&")).getOrElse("")
    }
    
    trait Routes {
        
        def routes:PartialFunction[Request,Action]
        
        //

        def badRequest(error:String) = new Action {
            def apply(ctx:Context) = BadRequest(error, contentType = "text/plain")
        }
        
        def call(generator: => Action) = {
            generator
        }

        def call[A](pa:Param[A])(generator: (A) => Action) = {
            pa.value.fold(badRequest, generator)
        }

        def call[A,B](pa:Param[A], pb:Param[B])(generator: (A,B) => Action) = {
            (for(a <- pa.value.right; b <- pb.value.right) yield (a,b)).fold(badRequest, {case (a,b) => generator(a,b)})
        }
        
        def actionFor(request:Request):Option[Action] = {
            routes.lift(request)
        }
        
        @scala.annotation.implicitNotFound("Cannot use a method returning ${T} as an action")
        trait ActionInvoker[T] {
            def call(call: => T, action:ActionDef):Action
        }

        object ActionInvoker {
            
            implicit def passThrought:ActionInvoker[Action] = new ActionInvoker[Action] {
                def call(call: => Action, action:ActionDef):Action = call
            }
            
            implicit def wrapJava:ActionInvoker[play.mvc.Result] = new ActionInvoker[play.mvc.Result] {
                def call(call: => play.mvc.Result, action:ActionDef) = {
                    new play.core.j.JavaAction { 
                        def invocation = call
                        def controller = action.ref.getClass.getClassLoader.loadClass(action.controller)
                        def method = controller.getDeclaredMethod(action.method, action.parameterTypes:_*)
                    }
                }
            }
            
        }
        
        def invokeAction[T](call: => T, action:ActionDef)(implicit d:ActionInvoker[T]):Action = {
            d.call(call, action)
        }
        
    }
    
}
