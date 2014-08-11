/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

import java.math.{ BigDecimal => JBigDec, BigInteger }

import java.util.{ Date, UUID }

/** Column mapping */
trait Column[A] extends ((Any, MetaDataItem) => MayErr[SqlRequestError, A])

/**
 * Column companion, providing default conversions.
 */
object Column {

  def apply[A](transformer: ((Any, MetaDataItem) => MayErr[SqlRequestError, A])): Column[A] = new Column[A] {

    def apply(value: Any, meta: MetaDataItem): MayErr[SqlRequestError, A] =
      transformer(value, meta)

  }

  def nonNull[A](transformer: ((Any, MetaDataItem) => MayErr[SqlRequestError, A])): Column[A] = Column[A] {
    case (value, meta @ MetaDataItem(qualified, _, _)) =>
      if (value != null) transformer(value, meta)
      else Left(UnexpectedNullableFound(qualified.toString))
  }

  implicit val columnToString: Column[String] =
    nonNull[String] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case string: String => Right(string)
        case clob: java.sql.Clob => Right(clob.getSubString(1, clob.length.asInstanceOf[Int]))
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to String for column $qualified"))
      }
    }

  /**
   * Column conversion to character.
   *
   * {{{
   * import anorm.SqlParser.scalar
   * import anorm.Column.columnToChar
   *
   * val c: Char = SQL("SELECT char FROM tbl").as(scalar[Char].single)
   * }}}
   */
  implicit val columnToChar: Column[Char] = nonNull[Char] { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case string: String => Right(string.charAt(0))
      case clob: java.sql.Clob => Right(clob.getSubString(1, 1).charAt(0))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Char for column $qualified"))
    }
  }

  implicit val columnToInt: Column[Int] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case int: Int => Right(int)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Int for column $qualified"))
    }
  }

  implicit val columnToFloat: Column[Float] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case f: Float => Right(f)
      case bi: BigInteger => Right(bi.floatValue)
      case i: Int => Right(i.toFloat)
      case s: Short => Right(s.toFloat)
      case b: Byte => Right(b.toFloat)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Float for column $qualified"))
    }
  }

  implicit val columnToDouble: Column[Double] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bg: JBigDec => Right(bg.doubleValue)
      case d: Double => Right(d)
      case f: Float => Right(new JBigDec(f.toString).doubleValue)
      case bi: BigInteger => Right(bi.doubleValue)
      case i: Int => Right(i.toDouble)
      case s: Short => Right(s.toDouble)
      case b: Byte => Right(b.toDouble)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Double for column $qualified"))
    }
  }

  implicit val columnToShort: Column[Short] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case b: Byte => Right(b.toShort)
      case s: Short => Right(s)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Short for column $qualified"))
    }
  }

  implicit val columnToByte: Column[Byte] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case b: Byte => Right(b)
      case s: Short => Right(s.toByte)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Byte for column $qualified"))
    }
  }

  implicit val columnToBoolean: Column[Boolean] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bool: Boolean => Right(bool)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Boolean for column $qualified"))
    }
  }

  implicit val columnToLong: Column[Long] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case int: Int => Right(int: Long)
      case long: Long => Right(long)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Long for column $qualified"))
    }
  }

  // Used to convert Java or Scala big integer
  private def anyToBigInteger(value: Any, meta: MetaDataItem): MayErr[SqlRequestError, BigInteger] = {
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: BigInteger => Right(bi)
      case int: Int => Right(BigInteger.valueOf(int))
      case long: Long => Right(BigInteger.valueOf(long))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to BigInteger for column $qualified"))
    }
  }

  /**
   * Column conversion to Java big integer.
   *
   * {{{
   * import anorm.SqlParser.scalar
   * import anorm.Column.columnToScalaBigInteger
   *
   * val c: BigInteger =
   *   SQL("SELECT COUNT(*) FROM tbl").as(scalar[BigInteger].single)
   * }}}
   */
  implicit val columnToBigInteger: Column[BigInteger] = nonNull(anyToBigInteger)

  /**
   * Column conversion to big integer.
   *
   * {{{
   * import anorm.SqlParser.scalar
   * import anorm.Column.columnToBigInt
   *
   * val c: BigInt =
   *   SQL("SELECT COUNT(*) FROM tbl").as(scalar[BigInt].single)
   * }}}
   */
  implicit val columnToBigInt: Column[BigInt] =
    nonNull((value, meta) => anyToBigInteger(value, meta).map(BigInt(_)))

  implicit val columnToUUID: Column[UUID] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case d: UUID => Right(d)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to UUID for column $qualified"))
    }
  }

  // Used to convert Java or Scala big decimal
  private def anyToBigDecimal(value: Any, meta: MetaDataItem): MayErr[SqlRequestError, JBigDec] = {
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      // TODO: Conversion from integer types
      case bi: JBigDec => Right(bi)
      case double: Double => Right(JBigDec.valueOf(double))
      case l: Long => Right(JBigDec.valueOf(l))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to BigDecimal for column $qualified"))
    }
  }

  /**
   * Column conversion to Java big decimal.
   *
   * {{{
   * import java.math.{ BigDecimal => JBigDecimal }
   * import anorm.SqlParser.scalar
   * import anorm.Column.columnToJavaBigDecimal
   *
   * val c: JBigDecimal =
   *   SQL("SELECT COUNT(*) FROM tbl").as(scalar[JBigDecimal].single)
   * }}}
   */
  implicit val columnToJavaBigDecimal: Column[JBigDec] =
    nonNull(anyToBigDecimal)

  /**
   * Column conversion to big decimal.
   *
   * {{{
   * import anorm.SqlParser.scalar
   * import anorm.Column.columnToScalaBigDecimal
   *
   * val c: BigDecimal =
   *   SQL("SELECT COUNT(*) FROM tbl").as(scalar[BigDecimal].single)
   * }}}
   */
  implicit val columnToScalaBigDecimal: Column[BigDecimal] =
    nonNull((value, meta) => anyToBigDecimal(value, meta).map(BigDecimal(_)))

  /**
   * Parses column as Java Date.
   * Time zone offset is the one of default JVM time zone
   * (see [[java.util.TimeZone.getDefault]]).
   *
   * {{{
   * import java.util.Date
   *
   * val d: Date = SQL("SELECT last_mod FROM tbl").as(scalar[Date].single)
   * }}}
   */
  implicit val columnToDate: Column[Date] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case date: Date => Right(date)
      case time: Long => Right(new Date(time))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Date for column $qualified"))
    }
  }

  /**
   * Parses column as joda DateTime
   *
   * {{{
   * import org.joda.time.DateTime
   *
   * val d: Date = SQL("SELECT last_mod FROM tbl").as(scalar[DateTime].single)
   * }}}
   */
  implicit val columnToJodaDateTime: Column[org.joda.time.DateTime] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case date: Date => Right(new org.joda.time.DateTime(date.getTime))
      case time: Long => Right(new org.joda.time.DateTime(time))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to DateTime for column $qualified"))
    }
  }

  /**
   * Parses column as joda Instant
   *
   * {{{
   * import org.joda.time.Instant
   *
   * val d: Date = SQL("SELECT last_mod FROM tbl").as(scalar[Instant].single)
   * }}}
   */
  implicit val columnToJodaInstant: Column[org.joda.time.Instant] = nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case date: Date => Right(new org.joda.time.Instant(date.getTime))
      case time: Long => Right(new org.joda.time.Instant(time))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Instant for column $qualified"))
    }
  }

  implicit def columnToPk[T](implicit c: Column[T]): Column[Pk[T]] =
    nonNull { (value, meta) => c(value, meta).map(Id(_)) }

  implicit def columnToOption[T](implicit transformer: Column[T]): Column[Option[T]] = Column { (value, meta) =>
    if (value != null) transformer(value, meta).map(Some(_)) else (Right(None): MayErr[SqlRequestError, Option[T]])
  }

  /**
   * Parses column as array.
   *
   * val a: Array[String] =
   *   SQL"SELECT str_arr FROM tbl".as(scalar[Array[String]])
   * }}}
   */
  implicit def columnToArray[T](implicit transformer: Column[T], t: scala.reflect.ClassTag[T]): Column[Array[T]] = Column { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta

    @annotation.tailrec
    def transf(a: Array[_], p: Array[T]): MayErr[SqlRequestError, Array[T]] =
      a.headOption match {
        case Some(r) => transformer(r, meta).toEither match {
          case Right(v) => transf(a.tail, p :+ v)
          case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to array for column $qualified"))
        }
        case _ => Right(p)
      }

    value match {
      case sql: java.sql.Array => try {
        transf(sql.getArray.asInstanceOf[Array[_]], Array[T]())
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to array for column $qualified"))
      }

      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to array for column $qualified"))
    }
  }

  /**
   * Parses column as list.
   *
   * val a: List[String] =
   *   SQL"SELECT str_arr FROM tbl".as(scalar[List[String]])
   * }}}
   */
  implicit def columnToList[T](implicit transformer: Column[T], t: scala.reflect.ClassTag[T]): Column[List[T]] = Column { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta

    @annotation.tailrec
    def transf(a: Array[_], p: List[T]): MayErr[SqlRequestError, List[T]] =
      a.headOption match {
        case Some(r) => transformer(r, meta).toEither match {
          case Right(v) => transf(a.tail, p :+ v)
          case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
        }
        case _ => Right(p)
      }

    value match {
      case sql: java.sql.Array => try {
        transf(sql.getArray.asInstanceOf[Array[_]], Nil)
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
      }

      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
    }
  }
}
