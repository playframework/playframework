/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

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

  implicit def columnToString: Column[String] = {
    Column.nonNull[String] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case string: String => Right(string)
        case clob: java.sql.Clob => Right(clob.getSubString(1, clob.length.asInstanceOf[Int]))
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value:${value.asInstanceOf[AnyRef].getClass} to String for column $qualified"))
      }
    }
  }

  implicit def columnToInt: Column[Int] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case int: Int => Right(int)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Int for column " + qualified))
    }
  }

  implicit def columnToDouble: Column[Double] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case d: Double => Right(d)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Double for column " + qualified))
    }
  }

  implicit def columnToShort: Column[Short] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case short: Short => Right(short)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Short for column " + qualified))
    }
  }

  implicit def columnToByte: Column[Byte] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case byte: Byte => Right(byte)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Byte for column " + qualified))
    }
  }

  implicit def columnToBoolean: Column[Boolean] =
    Column.nonNull { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case bool: Boolean => Right(bool)
        case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Boolean for column $qualified"))
      }
    }

  implicit def columnToLong: Column[Long] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case int: Int => Right(int: Long)
      case long: Long => Right(long)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to Long for column $qualified"))
    }
  }

  // Used to convert Java or Scala big integer
  private def anyToBigInteger(value: Any, meta: MetaDataItem): MayErr[SqlRequestError, java.math.BigInteger] = {
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: java.math.BigInteger => Right(bi)
      case int: Int => Right(java.math.BigInteger.valueOf(int))
      case long: Long => Right(java.math.BigInteger.valueOf(long))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value:${value.asInstanceOf[AnyRef].getClass} to BigInteger for column $qualified"))
    }
  }

  implicit def columnToBigInteger: Column[java.math.BigInteger] =
    Column.nonNull(anyToBigInteger)

  implicit def columnToBigInt: Column[BigInt] =
    Column.nonNull((value, meta) => anyToBigInteger(value, meta).map(BigInt(_)))

  implicit def columnToUUID: Column[UUID] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case d: UUID => Right(d)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to UUID for column " + qualified))
    }
  }

  // Used to convert Java or Scala big decimal
  private def anyToBigDecimal(value: Any, meta: MetaDataItem): MayErr[SqlRequestError, java.math.BigDecimal] = {
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: java.math.BigDecimal => Right(bi)
      case double: Double => Right(java.math.BigDecimal.valueOf(double))
      case l: Long => Right(java.math.BigDecimal.valueOf(l))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value:${value.asInstanceOf[AnyRef].getClass} to BigDecimal for column $qualified"))
    }
  }

  implicit def columnToJavaBigDecimal: Column[java.math.BigDecimal] =
    Column.nonNull(anyToBigDecimal)

  implicit def columnToScalaBigDecimal: Column[BigDecimal] =
    Column.nonNull((value, meta) =>
      anyToBigDecimal(value, meta).map(BigDecimal(_)))

  implicit def columnToDate: Column[Date] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case date: Date => Right(date)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Date for column " + qualified))
    }
  }

  implicit def columnToPk[T](implicit c: Column[T]): Column[Pk[T]] =
    Column.nonNull { (value, meta) => c(value, meta).map(Id(_)) }

  implicit def columnToOption[T](implicit transformer: Column[T]): Column[Option[T]] = Column { (value, meta) =>
    if (value != null) transformer(value, meta).map(Some(_)) else (Right(None): MayErr[SqlRequestError, Option[T]])
  }

}
