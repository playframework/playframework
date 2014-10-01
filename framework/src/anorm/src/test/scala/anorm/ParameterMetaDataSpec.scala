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

import java.sql.Timestamp

object ParameterMetaDataSpec extends org.specs2.mutable.Specification {
  "Parameter metadata" title

  "Metadata" should {
    shapeless.test.illTyped("implicitly[ParameterMetaData[Any]]")

    "be provided for parameter" >> {
      s"of type Boolean" in {
        Option(implicitly[ParameterMetaData[Boolean]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Java Boolean" in {
        Option(implicitly[ParameterMetaData[JBool]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Double" in {
        Option(implicitly[ParameterMetaData[Double]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Java Double" in {
        Option(implicitly[ParameterMetaData[JDouble]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Float" in {
        Option(implicitly[ParameterMetaData[Float]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Java Float" in {
        Option(implicitly[ParameterMetaData[JFloat]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Int" in {
        Option(implicitly[ParameterMetaData[Int]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Byte" in {
        Option(implicitly[ParameterMetaData[Byte]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Java Byte" in {
        Option(implicitly[ParameterMetaData[JByte]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Integer" in {
        Option(implicitly[ParameterMetaData[Integer]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Short" in {
        Option(implicitly[ParameterMetaData[Short]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Java Short" in {
        Option(implicitly[ParameterMetaData[JShort]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type BigInt" in {
        Option(implicitly[ParameterMetaData[BigInt]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type BigInteger" in {
        Option(implicitly[ParameterMetaData[BigInteger]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Long" in {
        Option(implicitly[ParameterMetaData[Long]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Java Long" in {
        Option(implicitly[ParameterMetaData[JLong]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type BigDecimal" in {
        Option(implicitly[ParameterMetaData[BigDecimal]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Java BigDecimal" in {
        Option(implicitly[ParameterMetaData[JBigDec]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Timestamp" in {
        Option(implicitly[ParameterMetaData[Timestamp]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type String" in {
        Option(implicitly[ParameterMetaData[String]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type UUID" in {
        Option(implicitly[ParameterMetaData[JUUID]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Char" in {
        Option(implicitly[ParameterMetaData[Char]].sqlType).
          aka("SQL type") must beSome
      }

      s"of type Character" in {
        Option(implicitly[ParameterMetaData[Character]].sqlType).
          aka("SQL type") must beSome
      }
    }
  }
}
