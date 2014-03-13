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

import java.math.{ BigDecimal => JBigDec }

import java.sql.{ PreparedStatement, Types }

/** Sets value as statement parameter. */
trait ToStatement[A] {

  /**
   * Sets value |v| on statement |s| at specified |index|.
   */
  def set(s: PreparedStatement, index: Int, v: A): Unit
}

/**
 * Provided conversions to set statement parameter.
 */
object ToStatement { // TODO: Scaladoc
  /**
   * Sets boolean value on statement.
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE enabled = {b}").on('b -> true)
   * }}}
   */
  implicit object booleanToStatement extends ToStatement[Boolean] {
    def set(s: PreparedStatement, i: Int, b: Boolean): Unit = s.setBoolean(i, b)
  }

  /**
   * Sets Java Boolean object on statement.
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE enabled = {b}").
   *   on('b -> java.lang.Boolean.TRUE)
   * }}}
   */
  implicit object javaBooleanToStatement extends ToStatement[JBool] {
    def set(s: PreparedStatement, i: Int, b: JBool): Unit =
      if (b != null) s.setBoolean(i, b) else s.setNull(i, Types.BOOLEAN)
  }

  /**
   * Sets byte value on statement.
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE flag = {b}").on('b -> 1.toByte)
   * }}}
   */
  implicit object byteToStatement extends ToStatement[Byte] {
    def set(s: PreparedStatement, i: Int, b: Byte): Unit = s.setByte(i, b)
  }

  /**
   * Sets Java Byte object on statement.
   *
   * {{{
   * SQL("SELECT * FROM Test WHERE flag = {b}").on('b -> new java.lang.Byte(1))
   * }}}
   */
  implicit object javaByteToStatement extends ToStatement[JByte] {
    def set(s: PreparedStatement, i: Int, b: JByte): Unit =
      if (b != null) s.setByte(i, b) else s.setNull(i, Types.SMALLINT)
  }

  implicit object doubleToStatement extends ToStatement[Double] {
    def set(s: PreparedStatement, i: Int, d: Double): Unit = s.setDouble(i, d)
  }

  implicit object javaDoubleToStatement extends ToStatement[JDouble] {
    def set(s: PreparedStatement, i: Int, d: JDouble): Unit =
      if (d != null) s.setDouble(i, d) else s.setNull(i, Types.DOUBLE)
  }

  implicit object floatToStatement extends ToStatement[Float] {
    def set(s: PreparedStatement, i: Int, f: Float): Unit = s.setFloat(i, f)
  }

  implicit object javaFloatToStatement extends ToStatement[JFloat] {
    def set(s: PreparedStatement, i: Int, f: JFloat): Unit =
      if (f != null) s.setFloat(i, f) else s.setNull(i, Types.FLOAT)
  }

  implicit object longToStatement extends ToStatement[Long] {
    def set(s: PreparedStatement, i: Int, l: Long): Unit = s.setLong(i, l)
  }

  implicit object javaLongToStatement extends ToStatement[JLong] {
    def set(s: PreparedStatement, i: Int, l: JLong): Unit =
      if (l != null) s.setLong(i, l) else s.setNull(i, Types.BIGINT)
  }

  implicit object intToStatement extends ToStatement[Int] {
    def set(s: PreparedStatement, i: Int, v: Int): Unit = s.setInt(i, v)
  }

  implicit object integerToStatement extends ToStatement[Integer] {
    def set(s: PreparedStatement, i: Int, v: Integer): Unit =
      if (v != null) s.setInt(i, v) else s.setNull(i, Types.INTEGER)
  }

  implicit object shortToStatement extends ToStatement[Short] {
    def set(s: PreparedStatement, i: Int, v: Short): Unit = s.setShort(i, v)
  }

  implicit object javaShortToStatement extends ToStatement[JShort] {
    def set(s: PreparedStatement, i: Int, v: JShort): Unit =
      if (v != null) s.setShort(i, v) else s.setNull(i, Types.SMALLINT)
  }

  @inline private def setChar(s: PreparedStatement, i: Int, ch: Character) {
    if (ch != null) s.setString(i, ch.toString) else s.setNull(i, Types.CHAR)
  }

  /**
   * Sets character as parameter value.
   *
   * {{{
   * SQL("SELECT * FROM tbl WHERE flag = {c}").on("c" -> 'f')
   * }}}
   */
  implicit object characterToStatement extends ToStatement[Character] {
    def set(s: PreparedStatement, i: Int, v: Character) = setChar(s, i, v)
  }

  implicit object stringToStatement extends ToStatement[String] {
    def set(s: PreparedStatement, i: Int, str: String): Unit =
      if (str != null) s.setString(i, str) else s.setNull(i, Types.VARCHAR)
  }

  implicit object charToStatement extends ToStatement[Char] {
    def set(s: PreparedStatement, i: Int, ch: Char): Unit = setChar(s, i, ch)
  }

  /**
   * Sets null for not assigned value.
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

  /**
   * Sets Java big integer on statement.
   *
   * {{{
   * SQL("UPDATE tbl SET max = {m}").on('m -> new java.math.BigInteger(15))
   * }}}
   */
  implicit object javaBigIntegerToStatement
      extends ToStatement[java.math.BigInteger] {
    def set(s: PreparedStatement, index: Int, v: java.math.BigInteger): Unit =
      s.setBigDecimal(index, new JBigDec(v))
  }

  /**
   * Sets big integer on statement.
   *
   * {{{
   * SQL("UPDATE tbl SET max = {m}").on('m -> BigInt(15))
   * }}}
   */
  implicit object scalaBigIntegerToStatement extends ToStatement[BigInt] {
    def set(s: PreparedStatement, index: Int, v: BigInt): Unit =
      s.setBigDecimal(index, new JBigDec(v.bigInteger))
  }

  /**
   * Sets Java big decimal on statement.
   *
   * {{{
   * SQL("UPDATE tbl SET max = {m}").on('m -> new java.math.BigDecimal(10.02f))
   * }}}
   */
  implicit object javaBigDecimalToStatement
      extends ToStatement[JBigDec] {
    def set(s: PreparedStatement, index: Int, v: JBigDec): Unit =
      s.setBigDecimal(index, v)
  }

  /**
   * Sets big decimal on statement.
   *
   * {{{
   * SQL("UPDATE tbl SET max = {m}").on('m -> BigDecimal(10.02f))
   * }}}
   */
  implicit object scalaBigDecimalToStatement extends ToStatement[BigDecimal] {
    def set(s: PreparedStatement, index: Int, v: BigDecimal): Unit =
      s.setBigDecimal(index, v.bigDecimal)
  }

  /**
   * Sets timestamp as statement parameter.
   *
   * {{{
   * SQL("UPDATE tbl SET modified = {ts}").
   *   on('ts -> new java.sql.Timestamp(date.getTime))
   * }}}
   */
  implicit object timestampToStatement extends ToStatement[java.sql.Timestamp] {
    def set(s: PreparedStatement, index: Int, ts: java.sql.Timestamp): Unit =
      s.setTimestamp(index, ts)
  }

  implicit object dateToStatement extends ToStatement[java.util.Date] {
    def set(s: PreparedStatement, index: Int, date: java.util.Date): Unit =
      s.setTimestamp(index, new java.sql.Timestamp(date.getTime))
  }

  implicit object uuidToStatement extends ToStatement[java.util.UUID] {
    def set(s: PreparedStatement, index: Int, id: java.util.UUID): Unit =
      s.setObject(index, id)
  }

  implicit object objectToStatement extends ToStatement[anorm.Object] {
    def set(s: PreparedStatement, index: Int, o: anorm.Object): Unit =
      s.setObject(index, o.value)
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
