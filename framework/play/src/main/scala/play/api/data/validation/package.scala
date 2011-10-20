package play.api.data

/**
 * Contains the format API used by Form.
 *
 * Example, defining a custom constraint:
 * {{{
 *   val negative = Constraint[Int] {
 *     case i if i < 0 => Valid
 *     case _ => Invalid("Must be a negative number.")
 *   }
 * }}}
 */
package object validation