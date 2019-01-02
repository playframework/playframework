/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler

import scala.collection.immutable.ListMap
import scala.util.matching.Regex

/**
 * Helper methods used in the templates
 */
package object templates {

  /**
   * Mark lines with source map information.
   */
  def markLines(routes: Rule*): String = {
    // since a compilation error is really not possible in a comment, there is no point in putting one line per
    // route, only the first one will ever be taken
    routes.headOption.fold("")("// @LINE:" + _.pos.line)
  }

  /**
   * Generate a base identifier for the given route
   */
  def baseIdentifier(route: Route, index: Int): String = route.call.packageName.map(_.replace(".", "_") + "_").getOrElse("") + route.call.controller.replace(".", "_") + "_" + route.call.method + index

  /**
   * Generate a route object identifier for the given route
   */
  def routeIdentifier(route: Route, index: Int): String = baseIdentifier(route, index) + "_route"

  /**
   * Generate a invoker object identifier for the given route
   */
  def invokerIdentifier(route: Route, index: Int): String = baseIdentifier(route, index) + "_invoker"

  /**
   * Generate a router object identifier
   */
  def routerIdentifier(include: Include, index: Int): String = include.router.replace(".", "_") + index

  def concatSep[T](seq: Seq[T], sep: String)(f: T => ScalaContent): Any = {
    if (seq.isEmpty) {
      Nil
    } else {
      Seq(f(seq.head), seq.tail.map { t =>
        Seq(sep, f(t))
      })
    }
  }

  /**
   * Generate a controller method call for the given route
   */
  def controllerMethodCall(r: Route, paramFormat: Parameter => String): String = {
    val methodPart = if (r.call.instantiate) {
      s"$Injector.instanceOf(classOf[${r.call.packageName.map(_ + ".").getOrElse("")}${r.call.controller}]).${r.call.method}"
    } else {
      s"${r.call.packageName.map(_ + ".").getOrElse("")}${r.call.controller}.${r.call.method}"
    }
    val paramPart = r.call.parameters.map { params =>
      params.map(paramFormat).mkString(", ")
    }.map("(" + _ + ")").getOrElse("")
    methodPart + paramPart
  }

  /**
   * Generate a controller method call for the given injected route
   */
  def injectedControllerMethodCall(r: Route, ident: String, paramFormat: Parameter => String): String = {
    val methodPart = if (r.call.instantiate) {
      s"$ident.get.${r.call.method}"
    } else {
      s"$ident.${r.call.method}"
    }
    val paramPart = r.call.parameters.map { params =>
      params.map(paramFormat).mkString(", ")
    }.map("(" + _ + ")").getOrElse("")
    methodPart + paramPart
  }

  def paramNameOnQueryString(paramName: String): String = {
    if (paramName.matches("^`[^`]+`$"))
      paramName.substring(1, paramName.length - 1)
    else
      paramName
  }
  /**
   * A route binding
   */
  def routeBinding(route: Route): String = {
    route.call.parameters.filterNot(_.isEmpty).map { params =>
      val ps = params.filterNot(_.isJavaRequest).map { p =>
        val paramName: String = paramNameOnQueryString(p.name)
        p.fixed.map { v =>
          """Param[""" + p.typeName + """]("""" + paramName + """", Right(""" + v + """))"""
        }.getOrElse {
          """params.""" + (if (route.path.has(paramName)) "fromPath" else "fromQuery") + """[""" + p.typeName + """]("""" + paramName + """", """ + p.default.map("Some(" + _ + ")").getOrElse("None") + """)"""
        }
      }
      if (ps.size < 22) ps.mkString(", ") else ps
    }.map("(" + _ + ")").filterNot(_ == "()").getOrElse("")
  }

  /**
   * Extract the local names out from the route, as tuple. See PR#4244
   */
  def tupleNames(route: Route): String = route.call.parameters.filterNot(_.isEmpty).map { params =>
    params.filterNot(_.isJavaRequest).map(x => safeKeyword(x.name)).mkString(", ")
  }.filterNot(_.isEmpty).map("(" + _ + ") =>").getOrElse("")

  /**
   * Extract the local names out from the route, as List. See PR#4244
   */
  def listNames(route: Route): String = route.call.parameters.filterNot(_.isEmpty).map { params =>
    params.filterNot(_.isJavaRequest).map(x => "(" + safeKeyword(x.name) + ": " + x.typeName + ")").mkString(":: ")
  }.filterNot(_.isEmpty).map("case " + _ + " :: Nil =>").getOrElse("")

  /**
   * Extract the local names out from the route
   */
  def localNames(route: Route): String =
    if (route.call.parameters.map(_.filterNot(_.isJavaRequest).size).getOrElse(0) < 22) tupleNames(route) else listNames(route)

  /**
   * The code to statically get the Play injector
   */
  val Injector = "play.api.Play.routesCompilerMaybeApplication.map(_.injector).getOrElse(play.api.inject.NewInstanceInjector)"

  val scalaReservedWords = List(
    "abstract", "case", "catch", "class",
    "def", "do", "else", "extends",
    "false", "final", "finally", "for",
    "forSome", "if", "implicit", "import",
    "lazy", "macro", "match", "new",
    "null", "object", "override", "package",
    "private", "protected", "return", "sealed",
    "super", "then", "this", "throw",
    "trait", "try", "true", "type",
    "val", "var", "while", "with",
    "yield",
    // Not scala keywords, but are used in the router
    "queryString"
  )

  /**
   * Ensure that the given keyword doesn't clash with any of the keywords that Play is using, including Scala keywords.
   */
  def safeKeyword(keyword: String): String =
    scalaReservedWords.collectFirst {
      case reserved if reserved == keyword => s"_pf_escape_$reserved"
    }.getOrElse(keyword)

  /**
   * Calculate the parameters for the reverse route call for the given routes.
   */
  def reverseParameters(routes: Seq[Route]): Seq[(Parameter, Int)] =
    routes.head.call.routeParams.zipWithIndex.filterNot {
      case (p, i) =>
        val fixeds = routes.map(_.call.routeParams(i).fixed).distinct
        fixeds.size == 1 && fixeds.head.isDefined
    }

  /**
   * Calculate the parameters for the javascript reverse route call for the given routes.
   */
  def reverseParametersJavascript(routes: Seq[Route]): Seq[(Parameter, Int)] =
    routes.head.call.routeParams.zipWithIndex.map {
      case (p, i) =>
        val re: Regex = """[^\p{javaJavaIdentifierPart}]""".r
        val paramEscapedName: String = re.replaceAllIn(p.name, "_")
        (p.copy(name = paramEscapedName + i), i)
    } filterNot {
      case (p, i) =>
        val fixeds = routes.map(_.call.routeParams(i).fixed).distinct
        fixeds.size == 1 && fixeds.head.isDefined
    }

  /**
   * Reverse parameters for matching
   */
  def reverseMatchParameters(params: Seq[(Parameter, Int)], annotateUnchecked: Boolean): String = {
    val annotation = if (annotateUnchecked) ": @unchecked" else ""
    params.map(x => safeKeyword(x._1.name) + annotation).mkString(", ")
  }

  /**
   * Generate the reverse parameter constraints
   *
   * In routes like /dummy controllers.Application.dummy(foo = "bar")
   * foo = "bar" is a constraint
   */
  def reverseParameterConstraints(route: Route, localNames: Map[String, String]): String = {
    route.call.parameters.getOrElse(Nil).filter { p =>
      localNames.contains(p.name) && p.fixed.isDefined
    }.map { p =>
      p.name + " == " + p.fixed.get
    } match {
      case Nil => ""
      case nonEmpty => "if " + nonEmpty.mkString(" && ")
    }
  }

  /**
   * Calculate the local names that need to be matched
   */
  def reverseLocalNames(route: Route, params: Seq[(Parameter, Int)]): Map[String, String] = params.map {
    case (lp, i) => route.call.routeParams(i).name -> lp.name
  }(scala.collection.breakOut)

  /**
   * Calculate the unique reverse constraints, and generate them using the given block
   */
  def reverseUniqueConstraints(routes: Seq[Route], params: Seq[(Parameter, Int)])(block: (Route, String, String, Map[String, String]) => ScalaContent): Seq[ScalaContent] = {
    ListMap(routes.reverse.map { route =>
      val localNames = reverseLocalNames(route, params)
      val parameters = reverseMatchParameters(params, annotateUnchecked = false)
      val parameterConstraints = reverseParameterConstraints(route, localNames)
      (parameters -> parameterConstraints) -> block(route, parameters, parameterConstraints, localNames)
    }: _*).values.toSeq.reverse
  }

  /**
   * Generate the reverse route context
   */
  def reverseRouteContext(route: Route): String = {
    val fixedParams = route.call.parameters.getOrElse(Nil).collect {
      case Parameter(name, _, Some(fixed), _) => "(\"%s\", %s)".format(name, fixed)
    }
    if (fixedParams.isEmpty) {
      ""
    } else {
      "implicit lazy val _rrc = new play.core.routing.ReverseRouteContext(Map(%s)); _rrc".format(fixedParams.mkString(", "))
    }
  }

  /**
   * Generate the parameter signature for the reverse route call for the given routes.
   */
  def reverseSignature(routes: Seq[Route]): String =
    reverseParameters(routes).map(p => safeKeyword(p._1.name) + ":" + p._1.typeName + {
      Option(routes.map(_.call.routeParams(p._2).default).distinct).filter(_.size == 1).flatMap(_.headOption).map {
        case None => ""
        case Some(default) => " = " + default
      }.getOrElse("")
    }).mkString(", ")

  /**
   * Generate the reverse call
   */
  def reverseCall(route: Route, localNames: Map[String, String] = Map()): String = {

    val df = if (route.path.parts.isEmpty) "" else " + { _defaultPrefix } + "
    val callPath = "_prefix" + df + route.path.parts.map {
      case StaticPart(part) => "\"" + part + "\""
      case DynamicPart(name, _, encode) =>
        route.call.routeParams.find(_.name == name).map { param =>
          val paramName: String = paramNameOnQueryString(param.name)
          val unbound = s"""implicitly[play.api.mvc.PathBindable[${param.typeName}]]""" +
            s""".unbind("$paramName", ${safeKeyword(localNames.getOrElse(param.name, param.name))})"""
          if (encode) s"play.core.routing.dynamicString($unbound)" else unbound
        }.getOrElse {
          throw new Error("missing key " + name)
        }
    }.mkString(" + ")

    val queryParams = route.call.routeParams.filterNot { p =>
      p.fixed.isDefined ||
        route.path.parts.collect {
          case DynamicPart(name, _, _) => name
        }.contains(p.name)
    }

    val callQueryString = if (queryParams.isEmpty) {
      ""
    } else {
      """ + play.core.routing.queryString(List(%s))""".format(
        queryParams.map { p =>
          ("""implicitly[play.api.mvc.QueryStringBindable[""" + p.typeName + """]].unbind("""" + paramNameOnQueryString(p.name) + """", """ + safeKeyword(localNames.getOrElse(p.name, p.name)) + """)""") -> p
        }.map {
          case (u, Parameter(name, typeName, None, Some(default))) =>
            """if(""" + safeKeyword(localNames.getOrElse(name, name)) + """ == """ + default + """) None else Some(""" + u + """)"""
          case (u, Parameter(name, typeName, None, None)) => "Some(" + u + ")"
        }.mkString(", "))

    }

    """Call("%s", %s%s)""".format(route.verb.value, callPath, callQueryString)
  }

  /**
   * Generate the Javascript code for the parameter constraints.
   *
   * This generates the contents of an if statement in JavaScript, and is used for when multiple routes route to the
   * same action but with different parameters.  If there are no constraints, None will be returned.
   */
  def javascriptParameterConstraints(route: Route, localNames: Map[String, String]): Option[String] = {
    Option(route.call.routeParams.filter { p =>
      localNames.contains(p.name) && p.fixed.isDefined
    }.map { p =>
      localNames(p.name) + " == \"\"\" + implicitly[play.api.mvc.JavascriptLiteral[" + p.typeName + "]].to(" + p.fixed.get + ") + \"\"\""
    }).filterNot(_.isEmpty).map(_.mkString(" && "))
  }

  /**
   * Collect all the routes that apply to a single action that are not dead.
   *
   * Dead routes occur when two routes route to the same action with the same parameters.  When reverse routing, this
   * means the one reverse router, depending on the parameters, will return different URLs.  But if they have the same
   * parameters, or no parameters, then after the first one, the subsequent ones will be dead code, never matching.
   *
   * This optimization not only saves on code generated, but since the body of the JavaScript router is a series of
   * very long String concatenation, this is hard work on the typer, which can easily stack overflow.
   */
  def javascriptCollectNonDeadRoutes(routes: Seq[Route]): Seq[(Route, Map[String, String], String)] = {
    routes.map { route =>
      val localNames = reverseLocalNames(route, reverseParametersJavascript(routes))
      val constraints = javascriptParameterConstraints(route, localNames)
      (route, localNames, constraints)
    }.foldLeft((Seq.empty[(Route, Map[String, String], String)], false)) {
      case ((_routes, true), dead) => (_routes, true)
      case ((_routes, false), (route, localNames, None)) => (_routes :+ ((route, localNames, "true")), true)
      case ((_routes, false), (route, localNames, Some(constraints))) => (_routes :+ ((route, localNames, constraints)), false)
    }._1
  }

  /**
   * Generate the Javascript call
   */
  def javascriptCall(route: Route, localNames: Map[String, String] = Map()): String = {
    val path = "\"\"\"\" + _prefix + " + { if (route.path.parts.isEmpty) "" else "{ _defaultPrefix } + " } + "\"\"\"\"" + route.path.parts.map {
      case StaticPart(part) => " + \"" + part + "\""
      case DynamicPart(name, _, encode) =>
        route.call.parameters.getOrElse(Nil).find(_.name == name).filterNot(_.isJavaRequest).map { param =>
          val paramName: String = paramNameOnQueryString(param.name)
          val jsUnbound =
            "(\"\"\" + implicitly[play.api.mvc.PathBindable[" + param.typeName + "]].javascriptUnbind + \"\"\")" +
              s"""("$paramName", ${localNames.getOrElse(param.name, param.name)})"""
          if (encode) s" + encodeURIComponent($jsUnbound)" else s" + $jsUnbound"
        }.getOrElse {
          throw new Error("missing key " + name)
        }
    }.mkString

    val queryParams = route.call.routeParams.filterNot { p =>
      p.fixed.isDefined ||
        route.path.parts.collect {
          case DynamicPart(name, _, _) => name
        }.contains(p.name)
    }

    val queryString = if (queryParams.isEmpty) {
      ""
    } else {
      """ + _qS([%s])""".format(
        queryParams.map { p =>
          val paramName: String = paramNameOnQueryString(p.name)
          ("(\"\"\" + implicitly[play.api.mvc.QueryStringBindable[" + p.typeName + "]].javascriptUnbind + \"\"\")" + """("""" + paramName + """", """ + localNames.getOrElse(p.name, p.name) + """)""") -> p
        }.map {
          case (u, Parameter(name, typeName, None, Some(default))) => """(""" + localNames.getOrElse(name, name) + " == null ? null : " + u + ")"
          case (u, Parameter(name, typeName, None, None)) => u
        }.mkString(", "))

    }

    "return _wA({method:\"%s\", url:%s%s})".format(route.verb.value, path, queryString)
  }

  /**
   * Generate the signature of a method on the ref router
   */
  def refReverseSignature(routes: Seq[Route]): String =
    routes.head.call.routeParams.map(p => safeKeyword(p.name) + ": " + p.typeName).mkString(", ")

  /**
   * Generate the ref router call
   */
  def refCall(route: Route, useInjector: Route => Boolean): String = {
    val controllerRef = s"${route.call.packageName.map(_ + ".").getOrElse("")}${route.call.controller}"
    val methodCall = s"${route.call.method}(${
      route.call.parameters.getOrElse(Nil).map(x => safeKeyword(x.nameClean)).mkString(", ")
    })"
    if (useInjector(route)) {
      s"$Injector.instanceOf(classOf[$controllerRef]).$methodCall"
    } else {
      s"$controllerRef.$methodCall"
    }
  }

  /**
   * Encode the given String constant as a triple quoted String.
   *
   * This will split the String at any $ characters, and use concatenation to concatenate a single $ String followed
   * be the remainder, this is to avoid "possible missing interpolator" false positive warnings.
   *
   * That is to say:
   *
   * {{{
   * /foo/$id<[^/]+>
   * }}}
   *
   * Will be encoded as:
   *
   * {{{
   *   """/foo/""" + "$" + """id<[^/]+>"""
   * }}}
   */
  def encodeStringConstant(constant: String): String = {
    constant.split('$').mkString(tq, s"""$tq + "$$" + $tq""", tq)
  }

  def groupRoutesByPackage(routes: Seq[Route]): Map[Option[String], Seq[Route]] = routes.groupBy(_.call.packageName)
  def groupRoutesByController(routes: Seq[Route]): Map[String, Seq[Route]] = routes.groupBy(_.call.controller)
  def groupRoutesByMethod(routes: Seq[Route]): Map[(String, Seq[String]), Seq[Route]] =
    routes.groupBy(r => (r.call.method, r.call.parameters.getOrElse(Nil).map(_.typeNameReal)))

  val ob = "{"
  val cb = "}"
  val tq = "\"\"\""
}
