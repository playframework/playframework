package anorm

import java.lang.{
  Boolean => JBool,
  Byte => JByte,
  Character,
  Double => JDouble,
  Float => JFloat,
  Long => JLong,
  Integer,
  Short => JShort
}

import java.sql.PreparedStatement

/** Set value as statement parameter. */
trait ToStatement[A] {
  def set(s: PreparedStatement, index: Int, aValue: A): Unit
}

/**
 * Provided conversions to set statement parameter.
 */
object ToStatement { // TODO: Scaladoc
  implicit object booleanToStatement extends ToStatement[Boolean] {
    def set(s: PreparedStatement, i: Int, b: Boolean): Unit = s.setBoolean(i, b)
  }

  implicit object javaBooleanToStatement extends ToStatement[JBool] {
    def set(s: PreparedStatement, i: Int, b: JBool): Unit = s.setBoolean(i, b)
  }

  implicit object byteToStatement extends ToStatement[Byte] {
    def set(s: PreparedStatement, i: Int, b: Byte): Unit = s.setByte(i, b)
  }

  implicit object javaByteToStatement extends ToStatement[JByte] {
    def set(s: PreparedStatement, i: Int, b: JByte): Unit = s.setByte(i, b)
  }

  implicit object doubleToStatement extends ToStatement[Double] {
    def set(s: PreparedStatement, i: Int, d: Double): Unit = s.setDouble(i, d)
  }

  implicit object javaDoubleToStatement extends ToStatement[JDouble] {
    def set(s: PreparedStatement, i: Int, d: JDouble): Unit = s.setDouble(i, d)
  }

  implicit object floatToStatement extends ToStatement[Float] {
    def set(s: PreparedStatement, i: Int, f: Float): Unit = s.setFloat(i, f)
  }

  implicit object javaFloatToStatement extends ToStatement[JFloat] {
    def set(s: PreparedStatement, i: Int, f: JFloat): Unit = s.setFloat(i, f)
  }

  implicit object longToStatement extends ToStatement[Long] {
    def set(s: PreparedStatement, i: Int, l: Long): Unit = s.setLong(i, l)
  }

  implicit object javaLongToStatement extends ToStatement[JLong] {
    def set(s: PreparedStatement, i: Int, l: JLong): Unit = s.setLong(i, l)
  }

  implicit object intToStatement extends ToStatement[Int] {
    def set(s: PreparedStatement, i: Int, v: Int): Unit = s.setInt(i, v)
  }

  implicit object integerToStatement extends ToStatement[Integer] {
    def set(s: PreparedStatement, i: Int, v: Integer): Unit = s.setInt(i, v)
  }

  implicit object shortToStatement extends ToStatement[Short] {
    def set(s: PreparedStatement, i: Int, v: Short): Unit = s.setShort(i, v)
  }

  implicit object javaShortToStatement extends ToStatement[JShort] {
    def set(s: PreparedStatement, i: Int, v: JShort): Unit = s.setShort(i, v)
  }

  implicit object characterToStatement extends ToStatement[Character] {
    def set(s: PreparedStatement, i: Int, v: Character): Unit =
      s.setString(i, v.toString)

  }

  implicit object stringToStatement extends ToStatement[String] {
    def set(s: PreparedStatement, index: Int, str: String): Unit =
      s.setString(index, str)
  }

  implicit object charToStatement extends ToStatement[Char] {
    def set(s: PreparedStatement, index: Int, ch: Char): Unit =
      s.setString(index, Character.toString(ch))
  }

  /**
   * Sets null for not assigned value
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE category = {c}")
   *   .on('c -> NotAssigned)
   * }}}
   */
  implicit object notAssignedToStatement extends ToStatement[NotAssigned.type] {
    def set(s: PreparedStatement, i: Int, n: NotAssigned.type): Unit =
      s.setObject(i, null)
  }

  /**
   * Sets null for None value.
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE category = {c}")
   *   .on('c -> None)
   * }}}
   */
  implicit object noneToStatement extends ToStatement[None.type] {
    def set(s: PreparedStatement, i: Int, n: None.type) = s.setObject(i, null)
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

  implicit object javaBigDecimalToStatement
      extends ToStatement[java.math.BigDecimal] {
    def set(s: PreparedStatement, index: Int, v: java.math.BigDecimal): Unit =
      s.setBigDecimal(index, v)
  }

  implicit object scalaBigDecimalToStatement extends ToStatement[BigDecimal] {
    def set(s: PreparedStatement, index: Int, v: BigDecimal): Unit =
      s.setBigDecimal(index, v.bigDecimal)
  }

  implicit object timestampToStatement extends ToStatement[java.sql.Timestamp] {
    def set(s: PreparedStatement, index: Int, ts: java.sql.Timestamp): Unit =
      s.setTimestamp(index, ts)
  }

  implicit object dateToStatement extends ToStatement[java.util.Date] {
    def set(s: PreparedStatement, index: Int, date: java.util.Date): Unit =
      s.setTimestamp(index, new java.sql.Timestamp(date.getTime()))
  }

  implicit object uuidToStatement extends ToStatement[java.util.UUID] {
    def set(s: PreparedStatement, index: Int, aValue: java.util.UUID): Unit =
      s.setObject(index, aValue)
  }

  implicit def idToStatement[A](implicit c: ToStatement[A]) =
    new ToStatement[Id[A]] {
      def set(s: PreparedStatement, index: Int, id: Id[A]): Unit =
        c.set(s, index, id.get)
    }

  implicit def seqToStatement[A](implicit c: ToStatement[A]) =
    new ToStatement[Seq[A]] {
      def set(s: PreparedStatement, offset: Int, ps: Seq[A]) {
        ps.foldLeft(offset) { (i, p) => c.set(s, i, p); i + 1 }
      }
    }

  implicit def seqParamToStatement[A](implicit c: ToStatement[Seq[A]]) =
    new ToStatement[SeqParameter[A]] {
      def set(s: PreparedStatement, offset: Int, ps: SeqParameter[A]): Unit =
        c.set(s, offset, ps.values)

    }
}
