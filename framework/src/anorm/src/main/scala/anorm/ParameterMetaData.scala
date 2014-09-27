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

import java.util.{ UUID => JUUID }

import java.math.{ BigDecimal => JBigDec, BigInteger }

import java.sql.{ Types, Timestamp }

/** Parameter meta data for type `T` */
trait ParameterMetaData[T] { // TODO: Move in separate file
  /**
   * Name of SQL type
   * @see [[java.sql.Types]]
   */
  def sqlType: String
}

/**
 * ParameterMetaData companion, providing defaults based on SQL92.
 */
object ParameterMetaData {
  import JodaParameterMetaData._

  /** Boolean parameter meta data */
  implicit object BooleanParameterMetaData extends ParameterMetaData[Boolean] {
    val sqlType = "BOOLEAN"
  }
  implicit object JBooleanParameterMetaData extends ParameterMetaData[JBool] {
    val sqlType = "BOOLEAN"
  }

  /** Double parameter meta data */
  implicit object DoubleParameterMetaData extends ParameterMetaData[Double] {
    val sqlType = "DOUBLE PRECISION"
  }
  implicit object JDoubleParameterMetaData extends ParameterMetaData[JDouble] {
    val sqlType = DoubleParameterMetaData.sqlType
  }

  /** Float parameter meta data */
  implicit object FloatParameterMetaData extends ParameterMetaData[Float] {
    val sqlType = "FLOAT"
  }
  implicit object JFloatParameterMetaData extends ParameterMetaData[JFloat] {
    val sqlType = FloatParameterMetaData.sqlType
  }

  /** Integer parameter meta data */
  implicit object IntParameterMetaData extends ParameterMetaData[Int] {
    val sqlType = "INTEGER"
  }
  implicit object ByteParameterMetaData extends ParameterMetaData[Byte] {
    val sqlType = IntParameterMetaData.sqlType
  }
  implicit object JByteParameterMetaData extends ParameterMetaData[JByte] {
    val sqlType = IntParameterMetaData.sqlType
  }
  implicit object IntegerParameterMetaData extends ParameterMetaData[Integer] {
    val sqlType = IntParameterMetaData.sqlType
  }
  implicit object ShortParameterMetaData extends ParameterMetaData[Short] {
    val sqlType = IntParameterMetaData.sqlType
  }
  implicit object JShortParameterMetaData extends ParameterMetaData[JShort] {
    val sqlType = IntParameterMetaData.sqlType
  }

  /** Numeric (big integer) parameter meta data */
  implicit object BigIntParameterMetaData extends ParameterMetaData[BigInt] {
    val sqlType = "NUMERIC"
  }
  implicit object BigIntegerParameterMetaData
      extends ParameterMetaData[BigInteger] {
    val sqlType = BigIntParameterMetaData.sqlType
  }
  implicit object LongParameterMetaData extends ParameterMetaData[Long] {
    val sqlType = BigIntParameterMetaData.sqlType
  }
  implicit object JLongParameterMetaData extends ParameterMetaData[JLong] {
    val sqlType = BigIntParameterMetaData.sqlType
  }

  /** Decimal (big decimal) parameter meta data */
  implicit object BigDecimalParameterMetaData
      extends ParameterMetaData[BigDecimal] {
    val sqlType = "DECIMAL"
  }
  implicit object JBigDecParameterMetaData extends ParameterMetaData[JBigDec] {
    val sqlType = BigDecimalParameterMetaData.sqlType
  }

  /** Timestamp parameter meta data */
  implicit object TimestampParameterMetaData
      extends ParameterMetaData[Timestamp] {
    val sqlType = "TIMESTAMP"
  }

  /** String/VARCHAR parameter meta data */
  implicit object StringParameterMetaData extends ParameterMetaData[String] {
    val sqlType = "VARCHAR"
  }
  implicit object UUIDParameterMetaData extends ParameterMetaData[JUUID] {
    val sqlType = StringParameterMetaData.sqlType
  }

  /** Character parameter meta data */
  implicit object CharParameterMetaData extends ParameterMetaData[Char] {
    val sqlType = "CHAR"
  }
  implicit object CharacterParameterMetaData
      extends ParameterMetaData[Character] {
    val sqlType = CharParameterMetaData.sqlType
  }
}
