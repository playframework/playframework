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
 * val param = ParameterValue("str", null, setter)
 *
 * SQL("...").onParams(param)
 * }}}
 */
object ParameterValue {
  import scala.language.implicitConversions

  private[anorm] trait Wrapper[T] { def value: T }

  @throws[IllegalArgumentException]("if value `v` is null whereas `toStmt` is marked with [[anorm.NotNullGuard]]") // TODO: MayErr on conversion to parameter values?
  def apply[A](v: A, s: ToSql[A], toStmt: ToStatement[A]) = (v, toStmt) match {
    case (null, _: NotNullGuard) => throw new IllegalArgumentException()
    case _ => new ParameterValue with Wrapper[A] {
      val value = v

      def toSql(stmt: String, o: Int): (String, Int) = {
        val frag: (String, Int) =
          if (s == null) ("?" -> 1) else s.fragment(value)

        Sql.rewrite(stmt, frag._1).fold[(String, Int)](
          /* ignore extra parameter */ stmt -> o)(rw =>
            (rw, o + frag._2))
      }

      def set(s: PreparedStatement, i: Int) = toStmt.set(s, i, value)

      override lazy val toString = s"ParameterValue($value)"
      override lazy val hashCode = value.hashCode

      override def equals(that: Any) = that match {
        case o: Wrapper[A] => (o.value == value)
        case _ => false
      }
    }
  }

  implicit def toParameterValue[A](a: A)(implicit s: ToSql[A] = null, p: ToStatement[A]): ParameterValue = apply(a, s, p)
}
