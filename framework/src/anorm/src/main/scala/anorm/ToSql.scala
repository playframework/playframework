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
  import scala.collection.immutable.SortedSet

  /**
   * Returns fragment for each value, separated by ", ".
   *
   * {{{
   * listToSql(List(1, 3, 5))
   * // "?, ?, ?"
   * }}}
   */
  implicit def listToSql[A](implicit conv: ToSql[A] = null): ToSql[List[A]] =
    traversableToSql[A, List[A]]

  /**
   * Returns fragment for each value, separated by ", ".
   *
   * {{{
   * seqToSql(Seq("A", "B", "C"))
   * // "?, ?, ?"
   * }}}
   */
  implicit def seqToSql[A](implicit conv: ToSql[A] = null): ToSql[Seq[A]] =
    traversableToSql[A, Seq[A]]

  /**
   * Returns fragment for each value, separated by ", ".
   *
   * {{{
   * setToSql(Set(1, 3, 5))
   * // "?, ?, ?"
   * }}}
   */
  implicit def setToSql[A](implicit conv: ToSql[A] = null): ToSql[Set[A]] =
    traversableToSql[A, Set[A]]

  /**
   * Returns fragment for each value, separated by ", ".
   *
   * {{{
   * sortedSetToSql(SortedSet("A", "B", "C"))
   * // "?, ?, ?"
   * }}}
   */
  implicit def sortedSetToSql[A](implicit conv: ToSql[A] = null): ToSql[SortedSet[A]] = traversableToSql[A, SortedSet[A]]

  /**
   * Returns fragment for each value, separated by ", ".
   *
   * {{{
   * streamToSql(Stream(1, 3, 5))
   * // "?, ?, ?"
   * }}}
   */
  implicit def streamToSql[A](implicit conv: ToSql[A] = null): ToSql[Stream[A]] = traversableToSql[A, Stream[A]]

  /**
   * Returns fragment for each value, separated by ", ".
   *
   * {{{
   * vectorToSql(Vector("A", "B", "C"))
   * // "?, ?, ?"
   * }}}
   */
  implicit def vectorToSql[A](implicit conv: ToSql[A] = null): ToSql[Vector[A]] = traversableToSql[A, Vector[A]]

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

  @inline private def traversableToSql[A, T <: Traversable[A]](implicit conv: ToSql[A] = null) = new ToSql[T] {
    def fragment(values: T): (String, Int) = {
      val c: A => (String, Int) =
        if (conv == null) _ => ("?" -> 1) else conv.fragment

      values.foldLeft("" -> 0) { (s, v) =>
        val frag = c(v)
        val st = if (s._2 > 0) ", " + frag._1 else frag._1
        (s._1 + st, s._2 + frag._2)
      }
    }
  }
}
