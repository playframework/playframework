/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.data

/**
 * Contains the Format API used by `Form`.
 *
 * For example, to define a custom formatter:
 * {{{
 * val signedIntFormat = new Formatter[Int] {
 *
 *   def bind(key: String, data: Map[String, String]) = {
 *     stringFormat.bind(key, data).flatMap { value =>
 *       scala.util.control.Exception.allCatch[Int]
 *         .either(java.lang.Integer.parseInt(value))
 *         .left.map(e => Seq(FormError(key, "error.signedNumber", Nil)))
 *     }
 *   }
 *
 *   def unbind(key: String, value: Long) = Map(
 *     key -> ((if (value<0) "-" else "+") + value)
 *   )
 * }
 * }}}
 */
package object format
