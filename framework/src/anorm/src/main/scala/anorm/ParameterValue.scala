package anorm

import java.sql.PreparedStatement

/** Prepared parameter value. */
sealed trait ParameterValue {

  /**
   * Writes placeholder(s) in [[java.sql.PreparedStatement]] syntax
   * (with '?') for this parameter in initial statement (with % placeholder).
   *
   * @param stmt SQL statement (with %s placeholders)
   * @param offset Position offset for this parameter
   * @return Update statement with '?' placeholder(s) for parameter,
   * with offset for next parameter
   */
  def toSql(stmt: String, offset: Int): (String, Int)

  /**
   * Sets this value on given statement at specified index.
   *
   * @param s SQL Statement
   * @param index Parameter index
   */
  def set(s: PreparedStatement, index: Int): Unit
}

/**
 * Value factory for parameter.
 *
 * {{{
 * val param = ParameterValue("str", setter)
 *
 * SQL("...").onParams(param)
 * }}}
 */
object ParameterValue {
  def apply[A](value: A, s: ToSql[A], toStmt: ToStatement[A]) =
    new ParameterValue {
      def toSql(stmt: String, o: Int): (String, Int) = {
        val frag: (String, Int) =
          if (s == null) ("?" -> 1) else s.fragment(value)

        Sql.rewrite(stmt, frag._1).fold[(String, Int)](
          /* ignore extra parameter */ stmt -> o)(rw =>
            (rw, o + frag._2))
      }

      def set(s: PreparedStatement, i: Int) = toStmt.set(s, i, value)
    }
}
