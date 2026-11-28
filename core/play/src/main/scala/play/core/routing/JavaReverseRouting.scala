/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import scala.annotation.varargs

/**
 * Helpers of the reverse routers that the routes compiler generates as Java source code.
 */
object JavaReverseRouting {

  /**
   * Encodes a dynamic part of a path.
   */
  def dynamicString(dynamic: String): String = play.core.routing.dynamicString(dynamic)

  /**
   * Joins the unbound query string parameters. A `null` item is a parameter that is left out, like one that has
   * its default value.
   */
  @varargs
  def queryString(items: String*): String = play.core.routing.queryString(items.map(Option(_)).toList)

  /**
   * Compares a reverse route argument with a fixed or default value of the routes file. The explicit type argument
   * makes the Java compiler check that the value has the type of the parameter.
   */
  def equal[T](value: T, other: T): Boolean = java.util.Objects.equals(value, other)

  /**
   * The context of a reverse route with the given fixed parameters, as alternating names and values.
   */
  @varargs
  def context(namesAndValues: AnyRef*): ReverseRouteContext =
    ReverseRouteContext(namesAndValues.grouped(2).map(pair => pair.head.toString -> pair.last).toMap)

  /**
   * The exception for reverse route arguments that no route of the action matches.
   */
  @varargs
  def noMatch(action: String, arguments: AnyRef*): IllegalArgumentException =
    new IllegalArgumentException(s"No route for $action matches the arguments ${arguments.mkString("[", ", ", "]")}")
}
