/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

import java.io.{ ByteArrayInputStream, InputStream }
import java.math.{ BigDecimal => JBigDec, BigInteger }
import java.util.{ Date, UUID }

import scala.util.{ Failure, Success => TrySuccess, Try }

import resource.managed

/** Column mapping */
trait Column[A] extends ((Any, MetaDataItem) => MayErr[SqlRequestError, A])

/** Column companion, providing default conversions. */
object Column extends JodaColumn {

  def apply[A](transformer: ((Any, MetaDataItem) => MayErr[SqlRequestError, A])): Column[A] = new Column[A] {

    def apply(value: Any, meta: MetaDataItem): MayErr[SqlRequestError, A] =
      transformer(value, meta)

  }

  @deprecated("Use [[nonNull1]]", "2.3.6")
  def nonNull[A](transformer: ((Any, MetaDataItem) => MayErr[SqlRequestError, A])): Column[A] = Column[A] {
    case (value, meta @ MetaDataItem(qualified, _, _)) =>
      if (value != null) transformer(value, meta)
      else MayErr(Left[SqlRequestError, A](
        UnexpectedNullableFound(qualified.toString)))
  }

  // TODO: Rename to nonNull
  /**
   * Helper function to implement column conversion.
   *
   * @param transformer Function converting raw value of column
   * @tparam Output type
   */
  def nonNull1[A](transformer: ((Any, MetaDataItem) => Either[SqlRequestError, A])): Column[A] = Column[A] {
    case (value, meta @ MetaDataItem(qualified, _, _)) =>
      MayErr(if (value != null) transformer(value, meta)
      else Left[SqlRequestError, A](
        UnexpectedNullableFound(qualified.toString)))

  }

  implicit val columnToString: Column[String] =
    nonNull1[String] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case string: String => Right(string)
        case clob: java.sql.Clob => Right(clob.getSubString(1, clob.length.asInstanceOf[Int]))
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to String for column $qualified"))
      }
    }

  /**
   * Column conversion to bytes array.
   *
   * {{{
   * import anorm.SqlParser.scalar
   * import anorm.Column.columnToByteArray
   *
   * val bytes: Array[Byte] = SQL("SELECT bin FROM tbl").
   *   as(scalar[Array[Byte]].single)
   * }}}
   */
  implicit val columnToByteArray: Column[Array[Byte]] =
    nonNull1[Array[Byte]] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case bytes: Array[Byte] => Right(bytes)
        case stream: InputStream => streamBytes(stream)
        case string: String => Right(string.getBytes)
        case blob: java.sql.Blob => streamBytes(blob.getBinaryStream)
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to bytes array for column $qualified"))
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
  implicit val columnToChar: Column[Char] = nonNull1[Char] { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case string: String => Right(string.charAt(0))
      case clob: java.sql.Clob => Right(clob.getSubString(1, 1).charAt(0))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Char for column $qualified"))
    }
  }

  implicit val columnToInt: Column[Int] = nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: BigInteger => Right(bi.intValue)
      case bd: JBigDec => Right(bd.intValue)
      case l: Long => Right(l.toInt)
      case i: Int => Right(i)
      case bool: Boolean => Right(if (!bool) 0 else 1)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Int for column $qualified"))
    }
  }

  /**
   * Column conversion to bytes array.
   *
   * {{{
   * import anorm.SqlParser.scalar
   * import anorm.Column.columnToInputStream
   *
   * val bytes: InputStream = SQL("SELECT bin FROM tbl").
   *   as(scalar[InputStream].single)
   * }}}
   */
  implicit val columnToInputStream: Column[InputStream] =
    nonNull1[InputStream] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case bytes: Array[Byte] => Right(new ByteArrayInputStream(bytes))
        case stream: InputStream => Right(stream)
        case string: String => Right(new ByteArrayInputStream(string.getBytes))
        case blob: java.sql.Blob => Right(blob.getBinaryStream)
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to input stream for column $qualified"))
      }
    }

  implicit val columnToFloat: Column[Float] = nonNull1 { (value, meta) =>
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

  implicit val columnToDouble: Column[Double] = nonNull1 { (value, meta) =>
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

  implicit val columnToShort: Column[Short] = nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case b: Byte => Right(b.toShort)
      case s: Short => Right(s)
      case bool: Boolean => Right(if (!bool) 0.toShort else 1.toShort)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Short for column $qualified"))
    }
  }

  implicit val columnToByte: Column[Byte] = nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case b: Byte => Right(b)
      case s: Short => Right(s.toByte)
      case bool: Boolean => Right(if (!bool) 0.toByte else 1.toByte)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Byte for column $qualified"))
    }
  }

  implicit val columnToBoolean: Column[Boolean] = nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bool: Boolean => Right(bool)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Boolean for column $qualified"))
    }
  }

  implicit val columnToLong: Column[Long] = nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: BigInteger => Right(bi.longValue)
      case bd: JBigDec => Right(bd.longValue)
      case int: Int => Right(int: Long)
      case long: Long => Right(long)
      case bool: Boolean => Right(if (!bool) 0l else 1l)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Long for column $qualified"))
    }
  }

  // Used to convert Java or Scala big integer
  private def anyToBigInteger(value: Any, meta: MetaDataItem): Either[SqlRequestError, BigInteger] = {
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: BigInteger => Right(bi)
      case bd: JBigDec => Right(bd.toBigInteger)
      case long: Long => Right(BigInteger.valueOf(long))
      case int: Int => Right(BigInteger.valueOf(int))
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
  implicit val columnToBigInteger: Column[BigInteger] =
    nonNull1(anyToBigInteger)

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
    nonNull1((value, meta) => anyToBigInteger(value, meta).right.map(BigInt(_)))

  implicit val columnToUUID: Column[UUID] = nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case d: UUID => Right(d)
      case s: String => Try { UUID.fromString(s) } match {
        case TrySuccess(v) => Right(v)
        case Failure(ex) => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to UUID for column $qualified"))
      }
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to UUID for column $qualified"))
    }
  }

  // Used to convert Java or Scala big decimal
  private def anyToBigDecimal(value: Any, meta: MetaDataItem): Either[SqlRequestError, JBigDec] = {
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      // TODO: Conversion from integer types
      case bi: JBigDec => Right(bi)
      case d: Double => Right(JBigDec.valueOf(d))
      case f: Float => Right(JBigDec.valueOf(f))
      case l: Long => Right(JBigDec.valueOf(l))
      case i: Int => Right(JBigDec.valueOf(i))
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
    nonNull1(anyToBigDecimal)

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
    nonNull1((value, meta) =>
      anyToBigDecimal(value, meta).right.map(BigDecimal(_)))

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
  implicit val columnToDate: Column[Date] = nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case date: Date => Right(date)
      case time: Long => Right(new Date(time))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Date for column $qualified"))
    }
  }

  implicit def columnToPk[T](implicit c: Column[T]): Column[Pk.Deprecated[T]] =
    nonNull { (value, meta) => c(value, meta).map(Id(_)) }

  implicit def columnToOption[T](implicit transformer: Column[T]): Column[Option[T]] = Column { (value, meta) =>
    if (value != null) transformer(value, meta).map(Some(_))
    else MayErr(Right[SqlRequestError, Option[T]](None))
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
    def transf(a: Array[_], p: Array[T]): Either[SqlRequestError, Array[T]] =
      a.headOption match {
        case Some(r) => transformer(r, meta).toEither match {
          case Right(v) => transf(a.tail, p :+ v)
          case Left(cause) => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to array for column $qualified: $cause"))
        }
        case _ => Right(p)
      }

    @annotation.tailrec
    def jiter(i: java.util.Iterator[_], p: Array[T]): Either[SqlRequestError, Array[T]] = if (!i.hasNext) Right(p)
    else transformer(i.next, meta).toEither match {
      case Right(v) => jiter(i, p :+ v)
      case Left(cause) => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified: $cause"))
    }

    MayErr[SqlRequestError, Array[T]](value match {
      case sql: java.sql.Array => try {
        transf(sql.getArray.asInstanceOf[Array[_]], Array.empty[T])
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to array for column $qualified"))
      }

      case arr: Array[_] => try {
        transf(arr, Array.empty[T])
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
      }

      case it: java.lang.Iterable[_] => try {
        jiter(it.iterator, Array.empty[T])
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert iterable $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
      }

      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to array for column $qualified"))
    })
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
    def transf(a: Array[_], p: List[T]): Either[SqlRequestError, List[T]] =
      a.headOption match {
        case Some(r) => transformer(r, meta).toEither match {
          case Right(v) => transf(a.tail, p :+ v)
          case Left(cause) => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified: $cause"))
        }
        case _ => Right(p)
      }

    @annotation.tailrec
    def jiter(i: java.util.Iterator[_], p: List[T]): Either[SqlRequestError, List[T]] = if (!i.hasNext) Right(p)
    else transformer(i.next, meta).toEither match {
      case Right(v) => jiter(i, p :+ v)
      case Left(cause) => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified: $cause"))
    }

    MayErr[SqlRequestError, List[T]](value match {
      case sql: java.sql.Array => try {
        transf(sql.getArray.asInstanceOf[Array[_]], Nil)
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert SQL array $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
      }

      case arr: Array[_] => try {
        transf(arr, Nil)
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert array $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
      }

      case it: java.lang.Iterable[_] => try {
        jiter(it.iterator, Nil)
      } catch {
        case _: Throwable => Left(TypeDoesNotMatch(s"Cannot convert iterable $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
      }

      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to list for column $qualified"))
    })
  }

  @inline private def streamBytes(in: InputStream): Either[SqlRequestError, Array[Byte]] = managed(in).acquireFor(streamToBytes(_)).fold({ errs =>
    Left(SqlMappingError(errs.headOption.
      fold("Fails to read binary stream")(_.getMessage)))
  }, Right(_))

  @annotation.tailrec
  private def streamToBytes(in: InputStream, bytes: Array[Byte] = Array(), buffer: Array[Byte] = Array.ofDim(1024)): Array[Byte] = {
    val count = in.read(buffer)

    if (count == -1) bytes
    else streamToBytes(in, bytes ++ buffer.take(count), buffer)
  }
}

import org.joda.time.{ DateTime, Instant }

sealed trait JodaColumn {
  /**
   * Parses column as joda DateTime
   *
   * {{{
   * import org.joda.time.DateTime
   *
   * val d: Date = SQL("SELECT last_mod FROM tbl").as(scalar[DateTime].single)
   * }}}
   */
  implicit val columnToJodaDateTime: Column[DateTime] = Column.nonNull1 {
    (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case date: Date => Right(new DateTime(date.getTime))
        case time: Long => Right(new DateTime(time))
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
  implicit val columnToJodaInstant: Column[Instant] = Column.nonNull1 {
    (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case date: Date => Right(new Instant(date.getTime))
        case time: Long => Right(new Instant(time))
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Instant for column $qualified"))
      }
  }
}
