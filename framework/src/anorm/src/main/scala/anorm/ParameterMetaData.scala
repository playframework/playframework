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

import java.util.{ Date, UUID => JUUID }

import java.math.{ BigDecimal => JBigDec, BigInteger }

import java.sql.{ Types, Timestamp }

/** Parameter meta data for type `T` */
trait ParameterMetaData[T] { // TODO: Move in separate file
  /**
   * Name of SQL type
   * @see [[java.sql.Types]]
   */
  def sqlType: String

  /**
   * JDBC type
   * @see [[java.sql.Types]]
   */
  def jdbcType: Int
}

/**
 * ParameterMetaData companion, providing defaults based on SQL92.
 */
object ParameterMetaData {
  import JodaParameterMetaData._

  /** Boolean parameter meta data */
  implicit object BooleanParameterMetaData extends ParameterMetaData[Boolean] {
    val sqlType = "BOOLEAN"
    val jdbcType = Types.BOOLEAN
  }
  implicit object JBooleanParameterMetaData extends ParameterMetaData[JBool] {
    val sqlType = BooleanParameterMetaData.sqlType
    val jdbcType = BooleanParameterMetaData.jdbcType
  }

  /** Double parameter meta data */
  implicit object DoubleParameterMetaData extends ParameterMetaData[Double] {
    val sqlType = "DOUBLE PRECISION"
    val jdbcType = Types.DOUBLE
  }
  implicit object JDoubleParameterMetaData extends ParameterMetaData[JDouble] {
    val sqlType = DoubleParameterMetaData.sqlType
    val jdbcType = DoubleParameterMetaData.jdbcType
  }

  /** Float parameter meta data */
  implicit object FloatParameterMetaData extends ParameterMetaData[Float] {
    val sqlType = "FLOAT"
    val jdbcType = Types.FLOAT
  }
  implicit object JFloatParameterMetaData extends ParameterMetaData[JFloat] {
    val sqlType = FloatParameterMetaData.sqlType
    val jdbcType = FloatParameterMetaData.jdbcType
  }

  /** Integer parameter meta data */
  implicit object IntParameterMetaData extends ParameterMetaData[Int] {
    val sqlType = "INTEGER"
    val jdbcType = Types.INTEGER
  }
  implicit object ByteParameterMetaData extends ParameterMetaData[Byte] {
    val sqlType = IntParameterMetaData.sqlType
    val jdbcType = Types.TINYINT
  }
  implicit object JByteParameterMetaData extends ParameterMetaData[JByte] {
    val sqlType = IntParameterMetaData.sqlType
    val jdbcType = ByteParameterMetaData.jdbcType
  }
  implicit object IntegerParameterMetaData extends ParameterMetaData[Integer] {
    val sqlType = IntParameterMetaData.sqlType
    val jdbcType = IntParameterMetaData.jdbcType
  }
  implicit object ShortParameterMetaData extends ParameterMetaData[Short] {
    val sqlType = IntParameterMetaData.sqlType
    val jdbcType = Types.SMALLINT
  }
  implicit object JShortParameterMetaData extends ParameterMetaData[JShort] {
    val sqlType = IntParameterMetaData.sqlType
    val jdbcType = ShortParameterMetaData.jdbcType
  }

  /** Numeric (big integer) parameter meta data */
  implicit object BigIntParameterMetaData extends ParameterMetaData[BigInt] {
    val sqlType = "NUMERIC"
    val jdbcType = Types.BIGINT
  }
  implicit object BigIntegerParameterMetaData
      extends ParameterMetaData[BigInteger] {
    val sqlType = BigIntParameterMetaData.sqlType
    val jdbcType = BigIntParameterMetaData.jdbcType
  }
  implicit object LongParameterMetaData extends ParameterMetaData[Long] {
    val sqlType = BigIntParameterMetaData.sqlType
    val jdbcType = BigIntParameterMetaData.jdbcType
  }
  implicit object JLongParameterMetaData extends ParameterMetaData[JLong] {
    val sqlType = BigIntParameterMetaData.sqlType
    val jdbcType = BigIntParameterMetaData.jdbcType
  }

  /** Decimal (big decimal) parameter meta data */
  implicit object BigDecimalParameterMetaData
      extends ParameterMetaData[BigDecimal] {
    val sqlType = "DECIMAL"
    val jdbcType = Types.DECIMAL
  }
  implicit object JBigDecParameterMetaData extends ParameterMetaData[JBigDec] {
    val sqlType = BigDecimalParameterMetaData.sqlType
    val jdbcType = BigDecimalParameterMetaData.jdbcType
  }

  /** Timestamp parameter meta data */
  implicit object TimestampParameterMetaData
      extends ParameterMetaData[Timestamp] {
    val sqlType = "TIMESTAMP"
    val jdbcType = Types.TIMESTAMP
  }
  implicit object DateParameterMetaData extends ParameterMetaData[Date] {
    val sqlType = TimestampParameterMetaData.sqlType
    val jdbcType = TimestampParameterMetaData.jdbcType
  }

  /** String/VARCHAR parameter meta data */
  implicit object StringParameterMetaData extends ParameterMetaData[String] {
    val sqlType = "VARCHAR"
    val jdbcType = Types.VARCHAR
  }
  implicit object UUIDParameterMetaData extends ParameterMetaData[JUUID] {
    val sqlType = StringParameterMetaData.sqlType
    val jdbcType = StringParameterMetaData.jdbcType
  }

  /** Character parameter meta data */
  implicit object CharParameterMetaData extends ParameterMetaData[Char] {
    val sqlType = "CHAR"
    val jdbcType = Types.CHAR
  }
  implicit object CharacterParameterMetaData
      extends ParameterMetaData[Character] {
    val sqlType = CharParameterMetaData.sqlType
    val jdbcType = CharParameterMetaData.jdbcType
  }
}
