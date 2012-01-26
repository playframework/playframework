package play.api.data

/**
 * Contains the Format API used by `Form`.
 *
 * For example, to define a custom formatter:
 * {{{
 * val signedIntFormat = new Formatter[Int] {
 *
 *   def bind(key: String, data: Map[String, String]) = {
 *     stringFormat.bind(key, data).right.flatMap { value =>
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