/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler

import java.io.File

import scala.util.parsing.input.Positional

/**
 * A routing rule
 */
sealed trait Rule extends Positional

/**
 * A route
 *
 * @param verb The verb (GET/POST etc)
 * @param path The path of the route
 * @param call The call to make
 * @param comments The comments above the route
 */
case class Route(
    verb: HttpVerb,
    path: PathPattern,
    call: HandlerCall,
    comments: Seq[Comment] = Seq.empty,
    modifiers: Seq[Modifier] = Seq.empty
) extends Rule

/**
 * An include for another router
 *
 * @param prefix The path prefix for the include
 * @param router The router to route to
 */
case class Include(prefix: String, router: String) extends Rule

/**
 * An HTTP verb
 */
case class HttpVerb(value: String) {
  override def toString = value
}

/**
 * A call to the handler.
 *
 * @param packageName The handlers package.
 * @param controller The controllers class name.
 * @param instantiate Whether the controller needs to be instantiated dynamically.
 * @param method The method to invoke on the controller.
 * @param parameters The parameters to pass to the method.
 */
case class HandlerCall(
    packageName: Option[String],
    controller: String,
    instantiate: Boolean,
    method: String,
    parameters: Option[Seq[Parameter]]
) extends Positional {
  private val dynamic                  = if (instantiate) "@" else ""
  lazy val routeParams: Seq[Parameter] = parameters.toIndexedSeq.flatten.filterNot(_.isJavaRequest)
  lazy val passJavaRequest: Boolean    = parameters.getOrElse(Nil).exists(_.isJavaRequest)
  override def toString                =
    dynamic + packageName.map(_ + ".").getOrElse("") + controller + dynamic + "." + method + parameters
      .map { params => "(" + params.mkString(", ") + ")" }
      .getOrElse("")
}

object Parameter {
  final val requestClass   = "Request"
  final val requestClassFQ = "play.mvc.Http." + requestClass
}

/**
 * A parameter for a controller method.
 *
 * @param name The name of the parameter.
 * @param typeName The type of the parameter.
 * @param fixed The fixed value for the parameter, if defined.
 * @param default A default value for the parameter, if defined.
 */
case class Parameter(name: String, typeName: String, fixed: Option[String], default: Option[String])
    extends Positional {
  import Parameter._

  def isJavaRequest = typeName == requestClass || typeName == requestClassFQ
  def typeNameReal  =
    if (isJavaRequest) {
      requestClassFQ
    } else {
      typeName
    }
  def nameClean =
    if (isJavaRequest) {
      "req"
    } else {
      name
    }
  override def toString =
    name + ":" + typeName + fixed.map(" = " + _).getOrElse("") + default.map(" ?= " + _).getOrElse("")
}

/**
 * A comment from the routes file.
 */
case class Comment(comment: String)

/**
 * A modifier tag in the routes file
 */
case class Modifier(value: String)

/**
 * A part of the path
 */
trait PathPart

/**
 * A dynamic part, which gets extracted into a parameter.
 *
 * @param name The name of the parameter that this part of the path gets extracted into.
 * @param constraint The regular expression used to match this part.
 * @param encode Whether this part should be encoded or not.
 */
case class DynamicPart(name: String, constraint: String, encode: Boolean) extends PathPart with Positional {
  override def toString =
    """DynamicPart("""" + name + "\", \"\"\"" + constraint + "\"\"\", encodeable=" + encode + ")" // "
}

/**
 * A static part of the path, which is matched as is.
 */
case class StaticPart(value: String) extends PathPart {
  override def toString = """StaticPart("""" + value + """")"""
}

/**
 * A complete path pattern, consisting of a sequence of path parts.
 */
case class PathPattern(parts: Seq[PathPart]) {

  /**
   * Whether this path pattern has a parameter by the given name.
   */
  def has(key: String): Boolean = parts.exists {
    case DynamicPart(name, _, _) if name == key => true
    case _                                      => false
  }

  override def toString =
    parts.map {
      case DynamicPart(name, constraint, encode) => "$" + name + "<" + constraint + ">"
      case StaticPart(path)                      => path
    }.mkString
}

/**
 * A routes compilation error
 *
 * @param source The source of the error
 * @param message The error message
 * @param line The line that the error occurred on
 * @param column The column that the error occurred on
 */
case class RoutesCompilationError(source: File, message: String, line: Option[Int], column: Option[Int])

/**
 * Information about the routes source file
 */
case class RoutesSourceInfo(source: String, date: String)
