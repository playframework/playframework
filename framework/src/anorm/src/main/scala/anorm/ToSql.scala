package anorm

/** Set value as prepared SQL statement fragment. */
trait ToSql[A] {

  /**
   * Prepares SQL fragment for value,
   * using [[java.sql.PreparedStatement]] syntax (with '?').
   *
   * @return SQL fragment and count of "?" placeholders in it
   */
  def fragment(value: A): (String, Int)
}

/** Provided ToSql implementations. */
object ToSql {
  import scala.language.implicitConversions

  /**
   * Returns fragment for each value, separated by ", ".
   *
   * {{{
   * seqToSql(Seq("A", "B", "C"))
   * // "?, ?, ?"
   * }}}
   */
  implicit def seqToSql[A](implicit conv: ToSql[A] = null) = new ToSql[Seq[A]] {
    def fragment(values: Seq[A]): (String, Int) = {
      val c: A => (String, Int) =
        if (conv == null) _ => ("?" -> 1) else conv.fragment

      values.foldLeft("" -> 0) { (s, v) =>
        val frag = c(v)
        val st = if (s._2 > 0) ", " + frag._1 else frag._1
        (s._1 + st, s._2 + frag._2)
      }
    }
  }

  /** Returns fragment for each value, with custom formatting. */
  implicit def seqParamToSql[A](implicit conv: ToSql[A] = null) =
    new ToSql[SeqParameter[A]] {
      def fragment(p: SeqParameter[A]): (String, Int) = {
        val before = p.before.getOrElse("")
        val after = p.after.getOrElse("")
        val c: A => (String, Int) =
          if (conv == null) _ => ("?" -> 1) else conv.fragment

        p.values.foldLeft("" -> 0) { (s, v) =>
          val frag = c(v)
          val st =
            if (s._2 > 0) p.separator + before + frag._1 else before + frag._1
          (s._1 + st + after, s._2 + frag._2)
        }
      }
    }
}
