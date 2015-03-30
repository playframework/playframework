/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.json

import java.time.{
  Clock,
  Instant,
  LocalDate,
  LocalDateTime,
  ZoneId,
  ZonedDateTime
}
import java.time.format.{ DateTimeFormatter, DateTimeParseException }
import java.time.temporal.UnsupportedTemporalTypeException

import scala.annotation.implicitNotFound
import scala.collection._
import scala.language.higherKinds

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ ArrayNode, ObjectNode }

import Json._
import play.api.data.validation.ValidationError
import reflect.ClassTag

/**
 * Json deserializer: write an implicit to define a deserializer for any type.
 */
@implicitNotFound(
  "No Json deserializer found for type ${A}. Try to implement an implicit Reads or Format for this type."
)
trait Reads[A] {
  self =>
  /**
   * Convert the JsValue into a A
   */
  def reads(json: JsValue): JsResult[A]

  def map[B](f: A => B): Reads[B] =
    Reads[B] { json => self.reads(json).map(f) }

  def flatMap[B](f: A => Reads[B]): Reads[B] = Reads[B] { json =>
    // Do not flatMap result to avoid repath
    self.reads(json) match {
      case JsSuccess(a, _) => f(a).reads(json)
      case error: JsError => error
    }
  }

  def filter(f: A => Boolean): Reads[A] =
    Reads[A] { json => self.reads(json).filter(f) }

  def filter(error: ValidationError)(f: A => Boolean): Reads[A] =
    Reads[A] { json => self.reads(json).filter(error)(f) }

  def filterNot(f: A => Boolean): Reads[A] =
    Reads[A] { json => self.reads(json).filterNot(f) }

  def filterNot(error: ValidationError)(f: A => Boolean): Reads[A] =
    Reads[A] { json => self.reads(json).filterNot(error)(f) }

  def collect[B](error: ValidationError)(f: PartialFunction[A, B]) =
    Reads[B] { json => self.reads(json).collect(error)(f) }

  def orElse(v: Reads[A]): Reads[A] =
    Reads[A] { json => self.reads(json).orElse(v.reads(json)) }

  def compose[B <: JsValue](rb: Reads[B]): Reads[A] =
    Reads[A] { js =>
      rb.reads(js) match {
        case JsSuccess(b, p) => this.reads(b).repath(p)
        case JsError(e) => JsError(e)
      }
    }

  def andThen[B](rb: Reads[B])(implicit witness: A <:< JsValue): Reads[B] = rb.compose(this.map(witness))

}

/**
 * Default deserializer type classes.
 */
object Reads extends ConstraintReads with PathReads with DefaultReads {

  val constraints: ConstraintReads = this

  val path: PathReads = this

  import play.api.libs.functional._

  implicit def applicative(implicit applicativeJsResult: Applicative[JsResult]): Applicative[Reads] = new Applicative[Reads] {

    def pure[A](a: A): Reads[A] = Reads[A] { _ => JsSuccess(a) }

    def map[A, B](m: Reads[A], f: A => B): Reads[B] = m.map(f)

    def apply[A, B](mf: Reads[A => B], ma: Reads[A]): Reads[B] = new Reads[B] { def reads(js: JsValue) = applicativeJsResult(mf.reads(js), ma.reads(js)) }

  }

  implicit def alternative(implicit a: Applicative[Reads]): Alternative[Reads] = new Alternative[Reads] {
    val app = a
    def |[A, B >: A](alt1: Reads[A], alt2: Reads[B]): Reads[B] = new Reads[B] {
      def reads(js: JsValue) = alt1.reads(js) match {
        case r @ JsSuccess(_, _) => r
        case r @ JsError(es1) => alt2.reads(js) match {
          case r2 @ JsSuccess(_, _) => r2
          case r2 @ JsError(es2) => JsError(JsError.merge(es1, es2))
        }
      }
    }
    def empty: Reads[Nothing] = new Reads[Nothing] { def reads(js: JsValue) = JsError(Seq()) }

  }

  def apply[A](f: JsValue => JsResult[A]): Reads[A] = new Reads[A] {
    def reads(json: JsValue) = f(json)
  }

  implicit def functorReads(implicit a: Applicative[Reads]) = new Functor[Reads] {
    def fmap[A, B](reads: Reads[A], f: A => B): Reads[B] = a.map(reads, f)
  }

  implicit object JsObjectMonoid extends Monoid[JsObject] {
    def append(o1: JsObject, o2: JsObject) = o1 deepMerge o2
    def identity = JsObject(Seq())
  }

  implicit val JsObjectReducer = Reducer[JsObject, JsObject](o => o)

  implicit object JsArrayMonoid extends Monoid[JsArray] {
    def append(a1: JsArray, a2: JsArray) = a1 ++ a2
    def identity = JsArray()
  }

  implicit val JsArrayReducer = Reducer[JsValue, JsArray](js => JsArray(Seq(js)))
}

/**
 * Default deserializer type classes.
 */
trait DefaultReads {
  import scala.language.implicitConversions

  /**
   * builds a JsErrorObj JsObject
   * {
   *    __VAL__ : "current known erroneous jsvalue",
   *    __ERR__ : "the i18n key of the error msg",
   *    __ARGS__ : "the args for the error msg" (JsArray)
   * }
   */
  def JsErrorObj(knownValue: JsValue, key: String, args: JsValue*) = {
    Json.obj(
      "__VAL__" -> knownValue,
      "__ERR__" -> key,
      "__ARGS__" -> args.foldLeft(JsArray())((acc: JsArray, arg: JsValue) => acc :+ arg)
    )
  }

  /**
   * Deserializer for Int types.
   */
  implicit object IntReads extends Reads[Int] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) if n.isValidInt => JsSuccess(n.toInt)
      case JsNumber(n) => JsError("error.expected.int")
      case _ => JsError("error.expected.jsnumber")
    }
  }

  /**
   * Deserializer for Short types.
   */
  implicit object ShortReads extends Reads[Short] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) if n.isValidShort => JsSuccess(n.toShort)
      case JsNumber(n) => JsError("error.expected.short")
      case _ => JsError("error.expected.jsnumber")
    }
  }

  /**
   * Deserializer for Byte types.
   */
  implicit object ByteReads extends Reads[Byte] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) if n.isValidByte => JsSuccess(n.toByte)
      case JsNumber(n) => JsError("error.expected.byte")
      case _ => JsError("error.expected.jsnumber")
    }
  }

  /**
   * Deserializer for Long types.
   */
  implicit object LongReads extends Reads[Long] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) if n.isValidLong => JsSuccess(n.toLong)
      case JsNumber(n) => JsError("error.expected.long")
      case _ => JsError("error.expected.jsnumber")
    }
  }

  /**
   * Deserializer for Float types.
   */
  implicit object FloatReads extends Reads[Float] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toFloat)
      case _ => JsError("error.expected.jsnumber")
    }
  }

  /**
   * Deserializer for Double types.
   */
  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => JsSuccess(n.toDouble)
      case _ => JsError("error.expected.jsnumber")
    }
  }

  /**
   * Deserializer for BigDecimal
   */
  implicit val bigDecReads = Reads[BigDecimal](js => js match {
    case JsString(s) =>
      scala.util.control.Exception.catching(classOf[NumberFormatException])
        .opt(JsSuccess(BigDecimal(new java.math.BigDecimal(s))))
        .getOrElse(JsError(ValidationError("error.expected.numberformatexception")))
    case JsNumber(d) => JsSuccess(d.underlying)
    case _ => JsError(ValidationError("error.expected.jsnumberorjsstring"))
  })

  /**
   * Deserializer for BigDecimal
   */
  implicit val javaBigDecReads = Reads[java.math.BigDecimal](js => js match {
    case JsString(s) =>
      scala.util.control.Exception.catching(classOf[NumberFormatException])
        .opt(JsSuccess(new java.math.BigDecimal(s)))
        .getOrElse(JsError(ValidationError("error.expected.numberformatexception")))
    case JsNumber(d) => JsSuccess(d.underlying)
    case _ => JsError(ValidationError("error.expected.jsnumberorjsstring"))
  })

  /**
   * Reads for the `java.util.Date` type.
   *
   * @param pattern a date pattern, as specified in `java.text.SimpleDateFormat`.
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks
   */
  def dateReads(pattern: String, corrector: String => String = identity): Reads[java.util.Date] = new Reads[java.util.Date] {

    def reads(json: JsValue): JsResult[java.util.Date] = json match {
      case JsNumber(d) => JsSuccess(new java.util.Date(d.toLong))
      case JsString(s) => parseJDate(pattern, corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() ->
          Seq(ValidationError("error.expected.date.isoformat", pattern))))
      }
      case _ => JsError(Seq(JsPath() ->
        Seq(ValidationError("error.expected.date"))))
    }
  }

  private def parseJDate(pattern: String, input: String): Option[java.util.Date] = {
    // REMEMBER THAT SIMPLEDATEFORMAT IS NOT THREADSAFE
    val df = new java.text.SimpleDateFormat(pattern)
    df.setLenient(false)
    try { Some(df.parse(input)) } catch {
      case x: java.text.ParseException =>
        println(s"=> $pattern -> ${x.getMessage}")
        None
    }
  }

  /**
   * the default implicit java.util.Date reads
   */
  implicit val DefaultDateReads = dateReads("yyyy-MM-dd")

  /** Typeclass to implement way of parsing string as Java8 temporal types. */
  trait TemporalParser[T <: java.time.temporal.Temporal] {
    def parse(input: String): Option[T]
  }

  /** Parsing companion */
  object TemporalParser {
    /** Instance of local date/time based on specified pattern. */
    implicit def LocalDateTimePatternParser(pattern: String): TemporalParser[LocalDateTime] = new TemporalParser[LocalDateTime] {
      def parse(input: String): Option[LocalDateTime] = try {
        Some(LocalDateTime.parse(input, DateTimeFormatter.ofPattern(pattern)))
      } catch {
        case _: DateTimeParseException => None
        case _: UnsupportedTemporalTypeException => None
      }
    }

    /** Instance of local date/time based on formatter. */
    implicit def LocalDateTimeFormatterParser(formatter: DateTimeFormatter): TemporalParser[LocalDateTime] = new TemporalParser[LocalDateTime] {
      def parse(input: String): Option[LocalDateTime] = try {
        Some(LocalDateTime.parse(input, formatter))
      } catch {
        case _: DateTimeParseException => None
        case _: UnsupportedTemporalTypeException => None
      }
    }

    /** Instance of date based on specified pattern. */
    implicit def DatePatternParser(pattern: String): TemporalParser[LocalDate] = new TemporalParser[LocalDate] {
      def parse(input: String): Option[LocalDate] = try {
        Some(LocalDate.parse(input, DateTimeFormatter.ofPattern(pattern)))
      } catch {
        case _: DateTimeParseException => None
        case _: UnsupportedTemporalTypeException => None
      }
    }

    /** Instance of date based on formatter. */
    implicit def DateFormatterParser(formatter: DateTimeFormatter): TemporalParser[LocalDate] = new TemporalParser[LocalDate] {
      def parse(input: String): Option[LocalDate] = try {
        Some(LocalDate.parse(input, formatter))
      } catch {
        case _: DateTimeParseException => None
        case _: UnsupportedTemporalTypeException => None
      }
    }

    /** Instance of instant parser based on specified pattern. */
    implicit def InstantPatternParser(pattern: String): TemporalParser[Instant] = new TemporalParser[Instant] {
      def parse(input: String): Option[Instant] = try {
        val time = LocalDateTime.parse(
          input, DateTimeFormatter.ofPattern(pattern))

        Some(Instant parse s"${time.toString}Z")
      } catch {
        case _: DateTimeParseException => None
        case _: UnsupportedTemporalTypeException => None
      }
    }

    /** Instance of instant parser based on formatter. */
    implicit def InstantFormatterParser(formatter: DateTimeFormatter): TemporalParser[Instant] = new TemporalParser[Instant] {
      def parse(input: String): Option[Instant] = try {
        val time = LocalDateTime.parse(input, formatter)
        Some(Instant parse s"${time.toString}Z")
      } catch {
        case _: DateTimeParseException => None
        case _: UnsupportedTemporalTypeException => None
      }
    }

    /** Instance of zoned date/time based on specified pattern. */
    implicit def ZonedDateTimePatternParser(pattern: String): TemporalParser[ZonedDateTime] = new TemporalParser[ZonedDateTime] {
      def parse(input: String): Option[ZonedDateTime] = try {
        Some(ZonedDateTime.parse(input, DateTimeFormatter.ofPattern(pattern)))
      } catch {
        case _: DateTimeParseException => try {
          Some(LocalDateTime.parse(input,
            DateTimeFormatter.ofPattern(pattern)).atZone(ZoneId.systemDefault))

        } catch {
          case _: DateTimeParseException => None
          case _: UnsupportedTemporalTypeException => None
        }
        case _: UnsupportedTemporalTypeException => None
      }
    }

    /** Instance of zoned date/time based on formatter. */
    implicit def ZonedDateTimeFormatterParser(formatter: DateTimeFormatter): TemporalParser[ZonedDateTime] = new TemporalParser[ZonedDateTime] {
      def parse(input: String): Option[ZonedDateTime] = try {
        Some(ZonedDateTime.parse(input, formatter))
      } catch {
        case _: DateTimeParseException => try {
          Some(LocalDateTime.parse(input, formatter).
            atZone(ZoneId.systemDefault))

        } catch {
          case _: DateTimeParseException => None
          case _: UnsupportedTemporalTypeException => None
        }
        case _: UnsupportedTemporalTypeException => None
      }
    }
  }

  /**
   * Reads for the `java.time.LocalDateTime` type.
   * When input value doesn't specify the time zone,
   * then `java.time.ZoneId.systemDefault` is used.
   *
   * @tparam T Type of argument to instantiate date/time parser
   * @param parsing Argument to instantiate date/time parser. Actually either a pattern (string) or a formatter (`java.time.format.DateTimeFormatter`)
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks. Function `identity` can be passed if no correction is needed.
   * @param p Typeclass instance based on `parsing`
   * @see [[TemporalFormatter]]
   *
   * {{{
   * import play.api.libs.json.Java8Reads.localDateTimeReads
   *
   * val customReads1 = localDateTimeReads("dd/MM/yyyy, HH:mm:ss")
   * val customReads2 = localDateTimeReads(DateTimeFormatter.ISO_DATE_TIME)
   * val customReads3 = localDateTimeReads(
   *   DateTimeFormatter.ISO_DATE_TIME, _.drop(1))
   * }}}
   */
  def localDateTimeReads[T](parsing: T, corrector: String => String = identity)(implicit p: T => TemporalParser[LocalDateTime]): Reads[LocalDateTime] = new Reads[LocalDateTime] {

    def reads(json: JsValue): JsResult[LocalDateTime] = json match {
      case JsNumber(d) => JsSuccess(epoch(d.toLong))
      case JsString(s) => p(parsing).parse(corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() ->
          Seq(ValidationError("error.expected.date.isoformat", parsing))))
      }
      case _ => JsError(Seq(JsPath() ->
        Seq(ValidationError("error.expected.date"))))
    }

    @inline def epoch(millis: Long): LocalDateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(millis), ZoneId.systemDefault)
  }

  /**
   * The default typeclass to reads `java.time.LocalDateTime` from JSON.
   * Accepts date/time formats as '2011-12-03T10:15:30', '2011-12-03T10:15:30+01:00' or '2011-12-03T10:15:30+01:00[Europe/Paris]'.
   */
  implicit val DefaultLocalDateTimeReads =
    localDateTimeReads(DateTimeFormatter.ISO_DATE_TIME)

  /**
   * Reads for the `java.time.ZonedDateTime` type.
   * When input value doesn't specify the time zone,
   * then `java.time.ZoneId.systemDefault` is used.
   *
   * @tparam T Type of argument to instantiate date/time parser
   * @param parsing Argument to instantiate date/time parser. Actually either a pattern (string) or a formatter (`java.time.format.DateTimeFormatter`)
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks. Function `identity` can be passed if no correction is needed.
   * @param p Typeclass instance based on `parsing`
   * @see [[TemporalFormatter]]
   *
   * {{{
   * import play.api.libs.json.Java8Reads.zonedDateTimeReads
   *
   * val customReads1 = zonedDateTimeReads("dd/MM/yyyy, HH:mm:ss")
   * val customReads2 = zonedDateTimeReads(DateTimeFormatter.ISO_DATE_TIME)
   * val customReads3 = zonedDateTimeReads(
   *   DateTimeFormatter.ISO_DATE_TIME, _.drop(1))
   * }}}
   */
  def zonedDateTimeReads[T](parsing: T, corrector: String => String = identity)(implicit p: T => TemporalParser[ZonedDateTime]): Reads[ZonedDateTime] = new Reads[ZonedDateTime] {

    def reads(json: JsValue): JsResult[ZonedDateTime] = json match {
      case JsNumber(d) => JsSuccess(epoch(d.toLong))
      case JsString(s) => p(parsing).parse(corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() ->
          Seq(ValidationError("error.expected.date.isoformat", parsing))))
      }
      case _ => JsError(Seq(JsPath() ->
        Seq(ValidationError("error.expected.date"))))
    }

    @inline def epoch(millis: Long): ZonedDateTime = ZonedDateTime.ofInstant(
      Instant.ofEpochMilli(millis), ZoneId.systemDefault)
  }

  /**
   * The default typeclass to reads `java.time.ZonedDateTime` from JSON.
   * Accepts date/time formats as '2011-12-03T10:15:30', '2011-12-03T10:15:30+01:00' or '2011-12-03T10:15:30+01:00[Europe/Paris]'.
   */
  implicit val DefaultZonedDateTimeReads =
    zonedDateTimeReads(DateTimeFormatter.ISO_DATE_TIME)

  /**
   * Reads for the `java.time.LocalDate` type.
   * When input value doesn't specify the time zone,
   * then `java.time.ZoneId.systemDefault` is used.
   *
   * @tparam T Type of argument to instantiate date parser
   * @param parsing Argument to instantiate date parser. Actually either a pattern (string) or a formatter (`java.time.format.DateTimeFormatter`)
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks. Function `identity` can be passed if no correction is needed.
   * @param p Typeclass instance based on `parsing`
   * @see [[TemporalFormatter]]
   *
   * {{{
   * import play.api.libs.json.Java8Reads.localDateReads
   *
   * val customReads1 = localDateReads("dd/MM/yyyy, HH:mm:ss")
   * val customReads2 = localDateReads(DateTimeFormatter.ISO_DATE)
   * val customReads3 = localDateReads(DateTimeFormatter.ISO_DATE, _.drop(1))
   * }}}
   */
  def localDateReads[T](parsing: T, corrector: String => String = identity)(implicit p: T => TemporalParser[LocalDate]): Reads[LocalDate] =
    new Reads[LocalDate] {
      def reads(json: JsValue): JsResult[LocalDate] = json match {
        case JsNumber(d) => JsSuccess(epoch(d.toLong))
        case JsString(s) => p(parsing).parse(corrector(s)) match {
          case Some(d) => JsSuccess(d)
          case None => JsError(Seq(JsPath() ->
            Seq(ValidationError("error.expected.date.isoformat", parsing))))
        }
        case _ => JsError(Seq(JsPath() ->
          Seq(ValidationError("error.expected.date"))))
      }

      @inline def epoch(millis: Long): LocalDate = LocalDate.now(
        Clock.fixed(Instant.ofEpochMilli(millis), ZoneId.systemDefault))
    }

  /**
   * The default typeclass to reads `java.time.LocalDate` from JSON.
   * Accepts date formats as '2011-12-03'.
   */
  implicit val DefaultLocalDateReads =
    localDateReads(DateTimeFormatter.ISO_DATE)

  /**
   * Reads for the `java.time.Instant` type.
   * When input value doesn't specify the time zone,
   * then `java.time.ZoneId.systemDefault` is used.
   *
   * @tparam T Type of argument to instantiate date parser
   * @param parsing Argument to instantiate date parser. Actually either a pattern (string) or a formatter (`java.time.format.DateTimeFormatter`)
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks. Function `identity` can be passed if no correction is needed.
   * @param p Typeclass instance based on `parsing`
   * @see [[TemporalFormatter]]
   *
   * {{{
   * import play.api.libs.json.Java8Reads.instantReads
   *
   * val customReads1 = instantReads("dd/MM/yyyy, HH:mm:ss")
   * val customReads2 = instantReads(DateTimeFormatter.ISO_INSTANT)
   * val customReads3 = instantReads(DateTimeFormatter.ISO_INSTANT, _.drop(1))
   * }}}
   */
  def instantReads[T](parsing: T, corrector: String => String = identity)(implicit p: T => TemporalParser[Instant]): Reads[Instant] =
    new Reads[Instant] {
      def reads(json: JsValue): JsResult[Instant] = json match {
        case JsNumber(d) => JsSuccess(Instant ofEpochMilli d.toLong)
        case JsString(s) => p(parsing).parse(corrector(s)) match {
          case Some(d) => JsSuccess(d)
          case None => JsError(Seq(JsPath() ->
            Seq(ValidationError("error.expected.date.isoformat", parsing))))
        }
        case _ => JsError(Seq(JsPath() ->
          Seq(ValidationError("error.expected.date"))))
      }
    }

  /**
   * The default typeclass to reads `java.time.Instant` from JSON.
   * Accepts instant formats as '2011-12-03T10:15:30', '2011-12-03T10:15:30+01:00' or '2011-12-03T10:15:30+01:00[Europe/Paris]'.
   */
  implicit val DefaultInstantReads =
    instantReads(DateTimeFormatter.ISO_DATE_TIME)

  /**
   * ISO 8601 Reads
   */
  object IsoDateReads extends Reads[java.util.Date] {
    import java.util.Date

    val millisAndTz = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
    val millis = "yyyy-MM-dd'T'HH:mm:ss.SSS"
    val tz = "yyyy-MM-dd'T'HH:mm:ssX"
    val mini = "yyyy-MM-dd'T'HH:mm:ss"

    val WithMillisAndTz = """^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}.+$""".r

    val WithMillis = """^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}$""".r

    val WithTz = """^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[^.]+$""".r

    def reads(json: JsValue): JsResult[Date] = json match {
      case JsNumber(d) => JsSuccess(new Date(d.toLong))
      case JsString(s) => (s match {
        case WithMillisAndTz() => millisAndTz -> parseJDate(millisAndTz, s)
        case WithMillis() => millis -> parseJDate(millis, s)
        case WithTz() => tz -> parseJDate(tz, s)
        case _ => mini -> parseJDate(mini, s)
      }) match {
        case (_, Some(d)) => JsSuccess(d)
        case (p, None) => JsError(Seq(JsPath() ->
          Seq(ValidationError("error.expected.date.isoformat", p))))
      }
    }
  }

  /**
   * Reads for the `org.joda.time.DateTime` type.
   *
   * @param pattern a date pattern, as specified in `java.text.SimpleDateFormat`.
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks
   */
  def jodaDateReads(pattern: String, corrector: String => String = identity): Reads[org.joda.time.DateTime] = new Reads[org.joda.time.DateTime] {
    import org.joda.time.DateTime

    val df = org.joda.time.format.DateTimeFormat.forPattern(pattern)

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsNumber(d) => JsSuccess(new DateTime(d.toLong))
      case JsString(s) => parseDate(corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format", pattern))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }

    private def parseDate(input: String): Option[DateTime] =
      scala.util.control.Exception.allCatch[DateTime] opt (DateTime.parse(input, df))

  }

  /**
   * the default implicit JodaDate reads
   */
  implicit val DefaultJodaDateReads = jodaDateReads("yyyy-MM-dd")

  /**
   * Reads for the `org.joda.time.LocalDate` type.
   *
   * @param pattern a date pattern, as specified in `org.joda.time.format.DateTimeFormat`.
   * @param corrector string transformation function (See jodaDateReads)
   */
  def jodaLocalDateReads(pattern: String, corrector: String => String = identity): Reads[org.joda.time.LocalDate] = new Reads[org.joda.time.LocalDate] {

    import org.joda.time.LocalDate
    import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }

    val df = if (pattern == "") ISODateTimeFormat.localDateParser else DateTimeFormat.forPattern(pattern)

    def reads(json: JsValue): JsResult[LocalDate] = json match {
      case JsString(s) => parseDate(corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format", pattern))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }

    private def parseDate(input: String): Option[LocalDate] =
      scala.util.control.Exception.allCatch[LocalDate] opt (LocalDate.parse(input, df))
  }

  /**
   * the default implicit joda.time.LocalDate reads
   */
  implicit val DefaultJodaLocalDateReads = jodaLocalDateReads("")

  /**
   * Reads for the `org.joda.time.LocalTime` type.
   *
   * @param pattern a date pattern, as specified in `org.joda.time.format.DateTimeFormat`.
   * @param corrector string transformation function (See jodaTimeReads)
   */
  def jodaLocalTimeReads(pattern: String, corrector: String => String = identity): Reads[org.joda.time.LocalTime] = new Reads[org.joda.time.LocalTime] {

    import org.joda.time.LocalTime
    import org.joda.time.format.{ DateTimeFormat, ISODateTimeFormat }

    val df = if (pattern == "") ISODateTimeFormat.localTimeParser else DateTimeFormat.forPattern(pattern)

    def reads(json: JsValue): JsResult[LocalTime] = json match {
      case JsNumber(n) => JsSuccess(new LocalTime(n.toLong))
      case JsString(s) => parseTime(corrector(s)) match {
        case Some(d) => JsSuccess(d)
        case None => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodatime.format", pattern))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.time"))))
    }

    private def parseTime(input: String): Option[LocalTime] =
      scala.util.control.Exception.allCatch[LocalTime] opt (LocalTime.parse(input, df))
  }

  /**
   * the default implicit joda.time.LocalTime reads
   */
  implicit val DefaultJodaLocalTimeReads = jodaLocalTimeReads("")

  /**
   * Reads for the `java.sql.Date` type.
   *
   * @param pattern a date pattern, as specified in `java.text.SimpleDateFormat`.
   * @param corrector a simple string transformation function that can be used to transform input String before parsing. Useful when standards are not exactly respected and require a few tweaks
   */
  def sqlDateReads(pattern: String, corrector: String => String = identity): Reads[java.sql.Date] =
    dateReads(pattern, corrector).map(d => new java.sql.Date(d.getTime))

  /**
   * the default implicit JodaDate reads
   */
  implicit val DefaultSqlDateReads = sqlDateReads("yyyy-MM-dd")

  /**
   * Reads for `scala.Enumeration` types using the name.
   *
   * @param enum a `scala.Enumeration`.
   */
  def enumNameReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue) = json match {
      case JsString(str) =>
        enum.values
          .find(_.toString == str)
          .map(JsSuccess(_))
          .getOrElse(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.validenumvalue")))))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.enumstring"))))
    }
  }

  /**
   * Deserializer for Boolean types.
   */
  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => JsSuccess(b)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsboolean"))))
    }
  }

  /**
   * Deserializer for String types.
   */
  implicit object StringReads extends Reads[String] {
    def reads(json: JsValue) = json match {
      case JsString(s) => JsSuccess(s)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }
  }

  /**
   * Deserializer for JsObject.
   */
  implicit object JsObjectReads extends Reads[JsObject] {
    def reads(json: JsValue) = json match {
      case o: JsObject => JsSuccess(o)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsobject"))))
    }
  }

  implicit object JsArrayReads extends Reads[JsArray] {
    def reads(json: JsValue) = json match {
      case o: JsArray => JsSuccess(o)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsarray"))))
    }
  }

  /**
   * Deserializer for JsValue.
   */
  implicit object JsValueReads extends Reads[JsValue] {
    def reads(json: JsValue) = JsSuccess(json)
  }

  implicit object JsStringReads extends Reads[JsString] {
    def reads(json: JsValue) = json match {
      case s: JsString => JsSuccess(s)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }
  }

  implicit object JsNumberReads extends Reads[JsNumber] {
    def reads(json: JsValue) = json match {
      case n: JsNumber => JsSuccess(n)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsnumber"))))
    }
  }

  implicit object JsBooleanReads extends Reads[JsBoolean] {
    def reads(json: JsValue) = json match {
      case b: JsBoolean => JsSuccess(b)
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsboolean"))))
    }
  }

  /**
   * Deserializer for Jackson JsonNode
   */
  implicit object JsonNodeReads extends Reads[JsonNode] {
    def reads(json: JsValue): JsResult[JsonNode] =
      JsSuccess(JacksonJson.jsValueToJsonNode(json))
  }

  /**
   * Deserializer for Jackson ObjectNode
   */
  implicit object ObjectNodeReads extends Reads[ObjectNode] {
    def reads(json: JsValue): JsResult[ObjectNode] = {
      json.validate[JsObject] map (jo => JacksonJson.jsValueToJsonNode(jo).asInstanceOf[ObjectNode])
    }
  }

  /**
   * Deserializer for Jackson ArrayNode
   */
  implicit object ArrayNodeReads extends Reads[ArrayNode] {
    def reads(json: JsValue): JsResult[ArrayNode] = {
      json.validate[JsArray] map (ja => JacksonJson.jsValueToJsonNode(ja).asInstanceOf[ArrayNode])
    }
  }

  /**
   * Deserializer for Map[String,V] types.
   */
  implicit def mapReads[V](implicit fmtv: Reads[V]): Reads[collection.immutable.Map[String, V]] = new Reads[collection.immutable.Map[String, V]] {
    def reads(json: JsValue) = json match {
      case JsObject(m) => {

        type Errors = Seq[(JsPath, Seq[ValidationError])]
        def locate(e: Errors, key: String) = e.map { case (p, valerr) => (JsPath \ key) ++ p -> valerr }

        m.foldLeft(Right(Map.empty): Either[Errors, Map[String, V]]) {
          case (acc, (key, value)) => (acc, fromJson[V](value)(fmtv)) match {
            case (Right(vs), JsSuccess(v, _)) => Right(vs + (key -> v))
            case (Right(_), JsError(e)) => Left(locate(e, key))
            case (Left(e), _: JsSuccess[_]) => Left(e)
            case (Left(e1), JsError(e2)) => Left(e1 ++ locate(e2, key))
          }
        }.fold(JsError.apply, res => JsSuccess(res.toMap))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsobject"))))
    }
  }

  /**
   * Generic deserializer for collections types.
   */
  implicit def traversableReads[F[_], A](implicit bf: generic.CanBuildFrom[F[_], A, F[A]], ra: Reads[A]) = new Reads[F[A]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) =>

        type Errors = Seq[(JsPath, Seq[ValidationError])]
        def locate(e: Errors, idx: Int) = e.map { case (p, valerr) => (JsPath(idx)) ++ p -> valerr }

        ts.zipWithIndex.foldLeft(Right(Vector.empty): Either[Errors, Vector[A]]) {
          case (acc, (elt, idx)) => (acc, fromJson[A](elt)(ra)) match {
            case (Right(vs), JsSuccess(v, _)) => Right(vs :+ v)
            case (Right(_), JsError(e)) => Left(locate(e, idx))
            case (Left(e), _: JsSuccess[_]) => Left(e)
            case (Left(e1), JsError(e2)) => Left(e1 ++ locate(e2, idx))
          }
        }.fold(JsError.apply, { res =>
          val builder = bf()
          res.foreach(builder.+=)
          JsSuccess(builder.result())
        })
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsarray"))))
    }
  }

  /**
   * Deserializer for Array[T] types.
   */
  implicit def ArrayReads[T: Reads: ClassTag]: Reads[Array[T]] = new Reads[Array[T]] {
    def reads(json: JsValue) = json.validate[List[T]].map(_.toArray)
  }

  /**
   * Deserializer for java.util.UUID
   */
  def uuidReader(checkUuuidValidity: Boolean = false): Reads[java.util.UUID] = new Reads[java.util.UUID] {
    import java.util.UUID
    import scala.util.Try
    def check(s: String)(u: UUID): Boolean = (u != null && s == u.toString())
    def parseUuid(s: String): Option[UUID] = {
      val uncheckedUuid = Try(UUID.fromString(s)).toOption

      if (checkUuuidValidity) {
        uncheckedUuid filter check(s)
      } else {
        uncheckedUuid
      }
    }

    def reads(json: JsValue) = json match {
      case JsString(s) => {
        parseUuid(s).map(JsSuccess(_)).getOrElse(JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.uuid")))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.uuid"))))
    }
  }

  implicit val uuidReads: Reads[java.util.UUID] = uuidReader()

}
