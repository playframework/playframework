/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler.javagen

import scala.collection.mutable

import play.routes.compiler._
import play.routes.compiler.templates._

/**
 * Helper methods of the templates that generate the routers as Java source code.
 *
 * The parameter types, fixed values and default values of the routes file are copied into the Java source code, so
 * the Java compiler checks them. Only the type syntax of the routes file is translated: `[]` becomes `<>`, the
 * bounds of wildcards become `extends` and `super`, and `_root_.` is removed.
 */
object JavaTemplates {
  private val Binders = "play.core.routing.JavaBinders."
  private val Reverse = "play.core.routing.JavaReverseRouting."

  /**
   * The context of a route without fixed parameters.
   */
  private val EmptyContext = "play.core.routing.ReverseRouteContext.empty()"

  private val boxedPrimitives = Map(
    "boolean" -> "java.lang.Boolean",
    "byte"    -> "java.lang.Byte",
    "char"    -> "java.lang.Character",
    "short"   -> "java.lang.Short",
    "int"     -> "java.lang.Integer",
    "long"    -> "java.lang.Long",
    "float"   -> "java.lang.Float",
    "double"  -> "java.lang.Double"
  )

  /**
   * The suffixes of the binders in `JavaBinders` by the names of their types.
   */
  private val binderSuffixes: Map[String, String] = Seq(
    "String"    -> Seq("String", "java.lang.String"),
    "Character" -> Seq("Character", "java.lang.Character", "char"),
    "Short"     -> Seq("Short", "java.lang.Short", "short"),
    "Integer"   -> Seq("Integer", "java.lang.Integer", "int"),
    "Long"      -> Seq("Long", "java.lang.Long", "long"),
    "Float"     -> Seq("Float", "java.lang.Float", "float"),
    "Double"    -> Seq("Double", "java.lang.Double", "double"),
    "Boolean"   -> Seq("Boolean", "java.lang.Boolean", "boolean"),
    "UUID"      -> Seq("UUID", "java.util.UUID")
  ).flatMap { case (suffix, names) => names.map(_ -> suffix) }.toMap

  private val optionalQueryBinderSuffixes: Map[String, String] = Seq("OptionalInt", "OptionalLong", "OptionalDouble")
    .flatMap(name => Seq(name -> name, s"java.util.$name" -> name))
    .toMap

  private val assetTypes = Set("Asset", "Assets.Asset", "controllers.Assets.Asset")

  private val optionalTypes = Set("Optional", "java.util.Optional")

  private val listTypes = Set("List", "java.util.List")

  private val javaKeywords = Set(
    "_",
    "abstract",
    "assert",
    "boolean",
    "break",
    "byte",
    "case",
    "catch",
    "char",
    "class",
    "const",
    "continue",
    "default",
    "do",
    "double",
    "else",
    "enum",
    "extends",
    "false",
    "final",
    "finally",
    "float",
    "for",
    "goto",
    "if",
    "implements",
    "import",
    "instanceof",
    "int",
    "interface",
    "long",
    "native",
    "new",
    "null",
    "package",
    "private",
    "protected",
    "public",
    "return",
    "short",
    "static",
    "strictfp",
    "super",
    "switch",
    "synchronized",
    "this",
    "throw",
    "throws",
    "transient",
    "true",
    "try",
    "void",
    "volatile",
    "while",
    // Not Java keywords, but used by the reverse routers
    "_prefix",
    "_defaultPrefix"
  )

  /**
   * A type of the routes file.
   */
  private sealed trait Type
  private case class Named(name: String, args: List[Type])              extends Type
  private case class Wildcard(lower: Option[Type], upper: Option[Type]) extends Type

  /**
   * A type that Java doesn't have, like a tuple or a type projection. It is copied as it is, so the Java compiler
   * reports it.
   */
  private case class Verbatim(text: String) extends Type

  private def parseType(typeName: String): Type = new TypeParser(typeName).parse().getOrElse(Verbatim(typeName))

  /**
   * Parses the types that the routes file parser accepts, like `java.util.Optional[java.util.List[String]]` or
   * `? >: Integer <: Number`.
   */
  private final class TypeParser(text: String) {
    private var pos = 0

    def parse(): Option[Type] = {
      val tpe = parseType()
      skipWhitespace()
      tpe.filter(_ => pos == text.length)
    }

    private def parseType(): Option[Type] = {
      skipWhitespace()
      if (text.startsWith("?", pos)) {
        pos += 1
        for {
          lower <- parseBound(">:")
          upper <- parseBound("<:")
        } yield Wildcard(lower, upper)
      } else {
        val start = pos
        while (pos < text.length && (Character.isJavaIdentifierPart(text(pos)) || text(pos) == '.')) pos += 1
        val name = text.substring(start, pos).trim.stripPrefix("_root_.")
        if (name.isEmpty) None
        else {
          skipWhitespace()
          if (text.startsWith("[", pos)) {
            pos += 1
            parseArgs(Nil).map(args => Named(name, args))
          } else Some(Named(name, Nil))
        }
      }
    }

    private def parseArgs(args: List[Type]): Option[List[Type]] = parseType().flatMap { arg =>
      skipWhitespace()
      if (text.startsWith(",", pos)) {
        pos += 1
        parseArgs(arg :: args)
      } else if (text.startsWith("]", pos)) {
        pos += 1
        Some((arg :: args).reverse)
      } else None
    }

    private def parseBound(operator: String): Option[Option[Type]] = {
      skipWhitespace()
      if (text.startsWith(operator, pos)) {
        pos += operator.length
        parseType().map(Some(_))
      } else Some(None)
    }

    private def skipWhitespace(): Unit = while (pos < text.length && text(pos).isWhitespace) pos += 1
  }

  private def render(tpe: Type): String = tpe match {
    case Named(name, Nil)       => name
    case Named(name, args)      => name + args.map(render).mkString("<", ", ", ">")
    case Wildcard(lower, upper) =>
      "?" + upper.map(" extends " + render(_)).getOrElse("") + lower.map(" super " + render(_)).getOrElse("")
    case Verbatim(text) => text
  }

  private def erasure(tpe: Type): String = tpe match {
    case Named(name, _) => name
    case Wildcard(_, _) => "java.lang.Object"
    case Verbatim(text) => text
  }

  /**
   * Mark the lines of all the given routes, like `markLines`.
   */
  def markRoutes(routes: Seq[Route]): String = markLines(routes*)

  /**
   * The Java type of a type of the routes file.
   */
  def javaType(typeName: String): String = render(parseType(typeName))

  /**
   * The Java type of a type of the routes file, with primitive types boxed, for type arguments and casts.
   */
  def boxedType(typeName: String): String = parseType(typeName) match {
    case Named(name, Nil) if boxedPrimitives.contains(name) => boxedPrimitives(name)
    case tpe                                                => render(tpe)
  }

  /**
   * The erasure of a type of the routes file, for class literals.
   */
  def erasedType(typeName: String): String = erasure(parseType(typeName))

  def javaImport(i: String): String = i.stripPrefix("_root_.")

  /**
   * A Java string literal.
   */
  def javaString(value: String): String =
    value.iterator
      .map {
        case '"'                     => "\\\""
        case '\\'                    => "\\\\"
        case '\n'                    => "\\n"
        case '\r'                    => "\\r"
        case '\t'                    => "\\t"
        case c if c < ' ' || c > '~' => f"\\u${c.toInt}%04x"
        case c                       => c.toString
      }
      .mkString("\"", "", "\"")

  /**
   * The path binder of a type of the routes file.
   *
   * @param context The reverse route context, which the binder of assets needs.
   */
  def pathBinder(typeName: String, context: String = EmptyContext): String = parseType(typeName) match {
    case Named(name, Nil) if binderSuffixes.contains(name) => Binders + "path" + binderSuffixes(name) + "()"
    case Named(name, Nil) if assetTypes.contains(name)     => Binders + s"pathAsset($context)"
    case tpe                                               => Binders + s"path(${erasure(tpe)}.class)"
  }

  /**
   * The query string binder of a type of the routes file.
   */
  def queryBinder(typeName: String): String = queryBinder(parseType(typeName))

  private def queryBinder(tpe: Type): String = tpe match {
    case Named(name, Nil) if binderSuffixes.contains(name)              => Binders + "query" + binderSuffixes(name) + "()"
    case Named(name, Nil) if optionalQueryBinderSuffixes.contains(name) =>
      Binders + "query" + optionalQueryBinderSuffixes(name) + "()"
    case Named(name, List(element)) if optionalTypes.contains(name) =>
      Binders + s"queryOptional(${queryBinder(element)})"
    case Named(name, List(element)) if listTypes.contains(name) => Binders + s"queryList(${queryBinder(element)})"
    case _                                                      => Binders + s"query(${erasure(tpe)}.class)"
  }

  /**
   * The JavaScript literal of the fixed value of a parameter. The cast to the parameter type picks the literal.
   */
  def javascriptLiteral(param: Parameter): String =
    Binders + s"javascriptLiteral((${boxedType(param.typeName)}) ${param.fixed.get})"

  /**
   * The Java identifiers of the parameters of a reverse route. They keep the names of the routes file where Java
   * allows them, so that Scala code can still pass arguments by name.
   */
  def javaIdentifiers(params: Seq[Parameter]): Seq[String] = {
    val identifiers = params.map { param =>
      val name       = paramNameOnQueryString(param.name)
      val identifier = name.map(c => if (Character.isJavaIdentifierPart(c)) c else '_')
      val start      =
        if (identifier.nonEmpty && Character.isJavaIdentifierStart(identifier.head)) identifier else "_" + identifier
      if (javaKeywords.contains(start)) "_pf_escape_" + start else start
    }
    identifiers.zipWithIndex.map {
      case (identifier, index) if identifiers.count(_ == identifier) > 1 => identifier + index
      case (identifier, _)                                               => identifier
    }
  }

  /**
   * Generate the registration of a route in the constructor of the router.
   */
  def javaRoute(route: Route, ident: String, indent: String): String = {
    val call       = route.call
    val controller = call.packageName.map(_ + ".").getOrElse("") + call.controller
    val parts      = route.path.parts.map {
      case StaticPart(value)                     => s"staticPart(${javaString(value)})"
      case DynamicPart(name, constraint, encode) =>
        s"dynamicPart(${javaString(name)}, ${javaString(constraint)}, $encode)"
      case part => throw new IllegalArgumentException(s"Unsupported path part $part")
    }
    val parameterTypes = call.parameters.getOrElse(Nil).map(p => erasedType(p.typeNameReal) + ".class")
    val params         = call.routeParams.filter(_.fixed.isEmpty).map { param =>
      val name    = paramNameOnQueryString(param.name)
      val default = param.default.map(", " + _).getOrElse("")
      if (route.path.has(name)) s"pathParam(${javaString(name)}, ${pathBinder(param.typeName)}$default)"
      else s"queryParam(${javaString(name)}, ${queryBinder(param.typeName)}$default)"
    }
    val calls = Seq(
      s".handler(${(javaString(controller) +: javaString(call.method) +: parameterTypes).mkString(", ")})",
      s".documentation(${javaString(call.toString)})"
    ) ++
      Some(route.comments.map(_.comment).mkString("\n")).filter(_.nonEmpty).map(c => s".comments(${javaString(c)})") ++
      Some(route.modifiers.map(m => javaString(m.value)))
        .filter(_.nonEmpty)
        .map(_.mkString(".modifiers(", ", ", ")")) ++
      Some(params).filter(_.nonEmpty).map(_.mkString(".params(", ", ", ")")) :+
      s".to(${javaAction(route, ident)});"
    val start = s"route(${(javaString(route.verb.value) +: javaString(route.path.toString) +: parts).mkString(", ")})"
    (start +: calls).mkString("\n" + indent + "  ")
  }

  /**
   * The lambda that calls the controller method of a route with the bound parameters.
   */
  private def javaAction(route: Route, ident: String): String = {
    val boundParams = route.call.routeParams.filter(_.fixed.isEmpty)
    val args        = route.call.parameters.getOrElse(Nil).map { param =>
      if (param.isJavaRequest) "req"
      else param.fixed.getOrElse(s"(${javaType(param.typeName)}) p[${boundParams.indexOf(param)}]")
    }
    val target = if (route.call.instantiate) s"$ident.get()" else ident
    val lambda = if (route.call.passJavaRequest) "(play.mvc.Http.Request req, Object[] p)" else "(Object[] p)"
    s"$lambda -> $target.${route.call.method}(${args.mkString(", ")})"
  }

  /**
   * The context of a reverse route with its fixed parameters, which the binder of assets needs.
   */
  private def reverseContext(route: Route): String = {
    val fixedParams = route.call.parameters.getOrElse(Nil).collect {
      case Parameter(name, _, Some(fixed), _) => s"${javaString(name)}, $fixed"
    }
    if (fixedParams.isEmpty) EmptyContext else Reverse + fixedParams.mkString("context(", ", ", ")")
  }

  /**
   * Generate the reverse call of a route.
   *
   * @param localName The Java identifier of a parameter of the route.
   */
  private def javaReverseCall(route: Route, localName: String => String): String = {
    val context   = reverseContext(route)
    val pathParts = route.path.parts.map {
      case StaticPart(part)             => javaString(part)
      case DynamicPart(name, _, encode) =>
        route.call.routeParams
          .find(_.name == name)
          .map { param =>
            val unbound = s"${pathBinder(param.typeName, context)}.unbind(" +
              s"${javaString(paramNameOnQueryString(param.name))}, ${localName(param.name)})"
            if (encode) s"${Reverse}dynamicString($unbound)" else unbound
          }
          .getOrElse(throw new Error("missing key " + name))
      case part => throw new IllegalArgumentException(s"Unsupported path part $part")
    }
    val path = ("_prefix.get()" +: (if (pathParts.isEmpty) Nil else "_defaultPrefix()" +: pathParts)).mkString(" + ")

    val dynamicNames = route.path.parts.collect { case DynamicPart(name, _, _) => name }
    val queryParams  = route.call.routeParams.filterNot(p => p.fixed.isDefined || dynamicNames.contains(p.name))
    val queryString  =
      if (queryParams.isEmpty) ""
      else
        queryParams
          .map { param =>
            val unbound = s"${queryBinder(param.typeName)}.unbind(" +
              s"${javaString(paramNameOnQueryString(param.name))}, ${localName(param.name)})"
            param.default.fold(unbound) { default =>
              s"${Reverse}<${boxedType(param.typeName)}>equal(${localName(param.name)}, $default) ? null : $unbound"
            }
          }
          .mkString(s" + ${Reverse}queryString(", ", ", ")")

    s"new play.api.mvc.Call(${javaString(route.verb.value)}, $path$queryString, null)"
  }

  private def sortedByRoutesFile[K](groups: Map[K, Seq[Route]]): Seq[(K, Seq[Route])] =
    groups.toSeq.sortBy { case (_, routes) => (routes.head.pos.line, routes.head.pos.column) }

  /**
   * Generate the methods of a reverse controller, with an overload for each trailing parameter that has the same
   * default value in all routes of the method.
   */
  def javaReverseMethods(routes: Seq[Route]): Seq[String] = {
    val methods = sortedByRoutesFile(groupRoutesByMethod(routes)).map {
      case ((method, _), routes) => new ReverseMethod(method, routes)
    }
    val signatures = mutable.Set(methods.map(_.signature)*)
    methods.flatMap { method =>
      method.source +: method.overloads.filter(overload => signatures.add(overload.signature)).map(_.source)
    }
  }

  private final class ReverseMethod(method: String, routes: Seq[Route]) {
    private val params      = reverseParameters(routes)
    private val identifiers = javaIdentifiers(params.map(_._1))
    private val types       = params.map(p => javaType(p._1.typeName))

    private val defaults = params.map {
      case (_, index) =>
        routes.map(_.call.routeParams(index).default).distinct match {
          case Seq(Some(default)) => Some(default)
          case _                  => None
        }
    }

    val signature: (String, Seq[String]) = (method, params.map(p => normalizedErasure(p._1.typeName)))

    /**
     * The routes of the method with the conditions on their fixed parameters. Routes with the same conditions as an
     * earlier route and routes after a route without conditions are left out, because they can't match, and Java
     * rejects unreachable code.
     */
    private def reachableRoutes: Seq[(Route, String => String, Seq[String])] = {
      val identifier = params.map(_._1.name).zip(identifiers).toMap
      val cases      = routes.map { route =>
        val localNames  = reverseLocalNames(route, params)
        val localName   = (name: String) => localNames.get(name).flatMap(identifier.get).getOrElse(name)
        val constraints = route.call.routeParams
          .filter(p => localNames.contains(p.name) && p.fixed.isDefined)
          .map(p => s"${Reverse}<${boxedType(p.typeName)}>equal(${localName(p.name)}, ${p.fixed.get})")
        (route, localName, constraints)
      }
      val distinct = cases.zipWithIndex.collect {
        case (routeCase, index) if cases.indexWhere(_._3 == routeCase._3) == index => routeCase
      }
      distinct.indexWhere(_._3.isEmpty) match {
        case -1    => distinct
        case index => distinct.take(index + 1)
      }
    }

    def source: String = {
      val reachable = reachableRoutes
      // The method already has the line marker of a single route
      val marker = (route: Route) => if (routes.size == 1) "" else s"    ${markLines(route)}\n"
      val body   = reachable.map {
        case (route, localName, Nil) =>
          s"""${marker(route)}    return ${javaReverseCall(route, localName)};"""
        case (route, localName, constraints) =>
          s"""${marker(route)}    if (${constraints.mkString(" && ")}) {
             |      return ${javaReverseCall(route, localName)};
             |    }""".stripMargin
      } ++ (if (reachable.exists(_._3.isEmpty)) Nil
            else Seq(s"    throw ${Reverse}noMatch(${(javaString(action) +: identifiers).mkString(", ")});"))
      val signature = types.zip(identifiers).map { case (tpe, identifier) => s"$tpe $identifier" }.mkString(", ")
      s"""
         |  ${markRoutes(routes)}
         |  public play.api.mvc.Call $method($signature) {
         |${body.mkString("\n")}
         |  }
         |""".stripMargin
    }

    private def action: String = {
      val call = routes.head.call
      call.packageName.map(_ + ".").getOrElse("") + call.controller + "." + method
    }

    /**
     * The overloads that leave out trailing parameters with default values.
     */
    def overloads: Seq[Overload] = {
      val trailing = defaults.reverseIterator.takeWhile(_.isDefined).size
      (1 to trailing).map { leftOut =>
        val kept = params.size - leftOut
        val args = identifiers.take(kept) ++ types.zip(defaults).drop(kept).map {
          case (tpe, default) => s"($tpe) ${default.get}"
        }
        val signature = types.zip(identifiers).take(kept).map { case (tpe, identifier) => s"$tpe $identifier" }
        Overload(
          (method, params.take(kept).map(p => normalizedErasure(p._1.typeName))),
          s"""
             |  ${markRoutes(routes)}
             |  public play.api.mvc.Call $method(${signature.mkString(", ")}) {
             |    return $method(${args.mkString(", ")});
             |  }
             |""".stripMargin
        )
      }
    }
  }

  private case class Overload(signature: (String, Seq[String]), source: String)

  private def normalizedErasure(typeName: String): String = {
    val erased = erasedType(typeName)
    erased.stripPrefix("java.lang.")
  }

  /**
   * A part of a JavaScript function: JavaScript text, or a Java expression that returns JavaScript text.
   */
  private sealed trait Js
  private case class JsText(text: String) extends Js
  private case class JsCode(code: String) extends Js

  /**
   * Generate the JavaScript call of a route, like the generated Scala JavaScript reverse routers do.
   */
  private def javascriptCall(route: Route, localNames: Map[String, String]): Seq[Js] = {
    val prefix = "_prefix.get()" + (if (route.path.parts.isEmpty) "" else " + _defaultPrefix()")
    val path   = Seq(JsText("\""), JsCode(prefix), JsText("\"")) ++ route.path.parts.flatMap {
      case StaticPart(part)             => Seq(JsText(" + \"" + part + "\""))
      case DynamicPart(name, _, encode) =>
        val param = route.call.parameters
          .getOrElse(Nil)
          .find(_.name == name)
          .filterNot(_.isJavaRequest)
          .getOrElse(throw new Error("missing key " + name))
        val unbound = Seq(
          JsText("("),
          JsCode(pathBinder(param.typeName) + ".javascriptUnbind()"),
          JsText(s""")("${paramNameOnQueryString(param.name)}", ${localNames.getOrElse(param.name, param.name)})""")
        )
        if (encode) JsText(" + encodeURIComponent(") +: unbound :+ JsText(")") else JsText(" + ") +: unbound
      case part => throw new IllegalArgumentException(s"Unsupported path part $part")
    }

    val dynamicNames = route.path.parts.collect { case DynamicPart(name, _, _) => name }
    val queryParams  = route.call.routeParams.filterNot(p => p.fixed.isDefined || dynamicNames.contains(p.name))
    val queryString  =
      if (queryParams.isEmpty) Nil
      else {
        val items = queryParams.map { param =>
          val localName = localNames.getOrElse(param.name, param.name)
          val unbound   = Seq(
            JsText("("),
            JsCode(queryBinder(param.typeName) + ".javascriptUnbind()"),
            JsText(s""")("${paramNameOnQueryString(param.name)}", $localName)""")
          )
          if (param.default.isDefined) JsText(s"($localName == null ? null : ") +: unbound :+ JsText(")") else unbound
        }
        JsText(" + _qS([") +: joined(items, JsText(", ")) :+ JsText("])")
      }

    JsText(s"""return _wA({method:"${route.verb.value}", url:""") +: (path ++ queryString :+ JsText("})"))
  }

  private def joined(parts: Seq[Seq[Js]], separator: Js): Seq[Js] =
    parts.reduce((left: Seq[Js], right: Seq[Js]) => (left :+ separator) ++ right)

  /**
   * The JavaScript conditions on the fixed parameters of a route, if it has any.
   */
  private def javascriptConstraints(route: Route, localNames: Map[String, String]): Option[Seq[Js]] = {
    val constraints = route.call.routeParams
      .filter(p => localNames.contains(p.name) && p.fixed.isDefined)
      .map(p => Seq(JsText(localNames(p.name) + " == "), JsCode(javascriptLiteral(p))))
    if (constraints.isEmpty) None else Some(joined(constraints, JsText(" && ")))
  }

  /**
   * Render JavaScript as a concatenation of Java expressions, with a Java string literal for each line.
   */
  private def javaConcatenation(js: Seq[Js], indent: String): String = {
    val merged = js
      .foldLeft(List.empty[Js]) {
        case (JsText(previous) :: rest, JsText(text)) => JsText(previous + text) :: rest
        case (rest, part)                             => part :: rest
      }
      .reverse
    val pieces = merged.flatMap {
      case JsText(text) => text.split("(?<=\n)").toSeq.filter(_.nonEmpty).map(t => (javaString(t), t.endsWith("\n")))
      case JsCode(code) => Seq((code, false))
    }
    pieces.zipWithIndex.map {
      case ((piece, _), 0)     => piece
      case ((piece, _), index) => (if (pieces(index - 1)._2) " +\n" + indent else " + ") + piece
    }.mkString
  }

  /**
   * Generate the methods of a JavaScript reverse controller.
   */
  def javaJavascriptReverseMethods(packageName: Option[String], controller: String, routes: Seq[Route]): Seq[String] =
    sortedByRoutesFile(groupRoutesByMethod(routes)).map {
      case ((method, _), routes) =>
        val params   = reverseParametersJavascript(routes)
        val function = JsText(s"\n        function(${params.map(_._1.name).mkString(",")}) {\n") +: (routes match {
          case Seq(route) =>
            JsText("          ") +: javascriptCall(route, reverseLocalNames(route, params)) :+ JsText("\n")
          case _ =>
            val nonDead = routes.iterator
              .map(route => (route, reverseLocalNames(route, params)))
              .map { case (route, localNames) => (route, localNames, javascriptConstraints(route, localNames)) }
              .toSeq
            val reachable = nonDead.indexWhere(_._3.isEmpty) match {
              case -1    => nonDead
              case index => nonDead.take(index + 1)
            }
            reachable.flatMap {
              case (route, localNames, constraints) =>
                Seq(JsText("\n          if (")) ++ constraints.getOrElse(Seq(JsText("true"))) ++
                  Seq(JsText(") {\n            ")) ++ javascriptCall(route, localNames) :+ JsText("\n          }\n")
            } :+ JsText("\n")
        }) :+ JsText("        }\n      ")
        val name = packageName.map(_ + ".").getOrElse("") + controller + "." + method
        s"""
           |  ${markRoutes(routes)}
           |  public play.api.routing.JavaScriptReverseRoute $method() {
           |    return new play.api.routing.JavaScriptReverseRoute(
           |      ${javaString(name)},
           |      ${javaConcatenation(function, "      ")}
           |    );
           |  }
           |""".stripMargin
    }
}
