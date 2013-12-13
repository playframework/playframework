package anorm

import java.sql.PreparedStatement

/** Set value as statement parameter. */
sealed trait ToStatement[A] {
  def set(s: PreparedStatement, index: Int, aValue: A): Unit
}

/**
 * Helper for parameter setters.
 */
object ToStatement {
  // TODO: Documentation
  implicit def anyParameter[T] = new ToStatement[T] {
    def set(s: PreparedStatement, i: Int, v: T): Unit = v match {
      case None | NotAssigned => s.setObject(i, null) // untyped null
      case o => s.setObject(i, o)
    }
  }

  implicit val booleanToStatement = new ToStatement[Boolean] {
    def set(s: PreparedStatement, i: Int, b: Boolean): Unit = s.setBoolean(i, b)
  }

  implicit val byteToStatement = new ToStatement[Byte] {
    def set(s: PreparedStatement, i: Int, b: Byte): Unit = s.setByte(i, b)
  }

  implicit val doubleToStatement = new ToStatement[Double] {
    def set(s: PreparedStatement, i: Int, d: Double): Unit = s.setDouble(i, d)
  }

  implicit val floatToStatement = new ToStatement[Float] {
    def set(s: PreparedStatement, i: Int, f: Float): Unit = s.setFloat(i, f)
  }

  implicit val longToStatement = new ToStatement[Long] {
    def set(s: PreparedStatement, i: Int, l: Long): Unit = s.setLong(i, l)
  }

  implicit val intToStatement = new ToStatement[Int] {
    def set(s: PreparedStatement, i: Int, v: Int): Unit = s.setInt(i, v)
  }

  implicit val shortToStatement = new ToStatement[Short] {
    def set(s: PreparedStatement, i: Int, v: Short): Unit = s.setShort(i, v)
  }

  implicit val stringToStatement = new ToStatement[String] {
    def set(s: PreparedStatement, index: Int, str: String): Unit =
      s.setString(index, str)
  }

  implicit val charToStatement = new ToStatement[Char] {
    def set(s: PreparedStatement, index: Int, ch: Char): Unit =
      s.setString(index, Character.toString(ch))
  }

  /**
   * Sets not empty optional A inferred as Some[A].
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE category = {c}")
   *   .on('c -> Some("cat"))
   * }}}
   */
  implicit def someToStatement[A](implicit c: ToStatement[A]) =
    new ToStatement[Some[A]] {
      def set(s: PreparedStatement, index: Int, v: Some[A]): Unit =
        c.set(s, index, v.get)
    }

  /**
   * Sets optional A inferred as Option[A].
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE category = {c}")
   *   .on('c -> Option("cat"))
   * }}}
   */
  implicit def optionToStatement[A >: Nothing](implicit c: ToStatement[A]) =
    new ToStatement[Option[A]] {
      def set(s: PreparedStatement, index: Int, o: Option[A]) =
        o.fold[Unit](s.setObject(index, null))(c.set(s, index, _))
      // TODO: Better null handling
    }

  implicit val javaBigDecimalToStatement =
    new ToStatement[java.math.BigDecimal] {
      def set(s: PreparedStatement, index: Int, v: java.math.BigDecimal): Unit =
        s.setBigDecimal(index, v)
    }

  implicit val scalaBigDecimalToStatement = new ToStatement[BigDecimal] {
    def set(s: PreparedStatement, index: Int, v: BigDecimal): Unit =
      s.setBigDecimal(index, v.bigDecimal)
  }

  implicit val timestampToStatement = new ToStatement[java.sql.Timestamp] {
    def set(s: PreparedStatement, index: Int, ts: java.sql.Timestamp): Unit =
      s.setTimestamp(index, ts)
  }

  implicit val dateToStatement = new ToStatement[java.util.Date] {
    def set(s: PreparedStatement, index: Int, date: java.util.Date): Unit =
      s.setTimestamp(index, new java.sql.Timestamp(date.getTime()))
  }

  implicit val uuidToStatement = new ToStatement[java.util.UUID] {
    def set(s: PreparedStatement, index: Int, aValue: java.util.UUID): Unit =
      s.setObject(index, aValue)
  }

  implicit def idToStatement[A](implicit c: ToStatement[A]) =
    new ToStatement[Id[A]] {
      def set(s: PreparedStatement, index: Int, id: Id[A]): Unit =
        c.set(s, index, id.get)
    }
}
