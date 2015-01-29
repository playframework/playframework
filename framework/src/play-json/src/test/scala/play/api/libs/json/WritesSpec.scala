package play.api.libs.json

import java.time.{
  Instant,
  LocalDateTime,
  LocalDate,
  ZonedDateTime,
  ZoneId
}
import java.time.format.DateTimeFormatter

object WritesSpec extends org.specs2.mutable.Specification {
  "JSON Writes" title

  val testZone = ZoneId.of("UTC")

  "Local date/time" should {
    val DefaultWrites = implicitly[Writes[LocalDateTime]]
    import DefaultWrites.writes

    @inline def dateTime(input: String) =
      LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)

    val CustomWrites1 = Writes.
      temporalWrites[LocalDateTime, String]("dd/MM/yyyy, HH:mm:ss")

    "be written as number" in {
      Writes.LocalDateTimeNumberWrites.writes(LocalDateTime.ofInstant(
        Instant.ofEpochMilli(1234567890L), testZone)).
        aka("written date") must_== JsNumber(BigDecimal valueOf 1234567000L)
    }

    "be written with default implicit as '2011-12-03T10:15:30'" in {
      writes(dateTime("2011-12-03T10:15:30")) aka "written date" must_== (
        JsString("2011-12-03T10:15:30"))
    }

    "be written with custom pattern as '03/12/2011, 10:15:30'" in {
      CustomWrites1.writes(dateTime("2011-12-03T10:15:30")).
        aka("written date") must_== JsString("03/12/2011, 10:15:30")
    }
  }

  "Zoned date/time" should {
    val DefaultWrites = implicitly[Writes[ZonedDateTime]]
    import DefaultWrites.writes

    @inline def dateTime(input: String) = try {
      ZonedDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)
    } catch {
      case _: Throwable => LocalDateTime.parse(
        input, DateTimeFormatter.ISO_DATE_TIME).atZone(testZone)
    }

    val CustomWrites1 = Writes.
      temporalWrites[ZonedDateTime, String]("dd/MM/yyyy, HH:mm:ss")

    "be written as number" in {
      Writes.ZonedDateTimeNumberWrites.writes(ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(1234567890L), testZone)).
        aka("written date") must_== JsNumber(BigDecimal valueOf 1234567890L)
    }

    "be written with default implicit as '2011-12-03T10:15:30'" in {
      writes(dateTime("2011-12-03T10:15:30")) aka "written date" must_== (
        JsString("2011-12-03T10:15:30"))
    }

    "be written with custom pattern as '03/12/2011, 10:15:30'" in {
      CustomWrites1.writes(dateTime("2011-12-03T10:15:30")).
        aka("written date") must_== JsString("03/12/2011, 10:15:30")
    }
  }

  "Local date" should {
    val DefaultWrites = implicitly[Writes[LocalDate]]
    import DefaultWrites.writes

    @inline def date(input: String) =
      LocalDate.parse(input, DateTimeFormatter.ISO_DATE)

    val CustomWrites1 = Writes.temporalWrites[LocalDate, String]("dd/MM/yyyy")

    "be written as number" in {
      Writes.LocalDateNumberWrites.writes(
        LocalDate ofEpochDay 1234567890L) aka "written date" must_== JsNumber(
          BigDecimal valueOf 106666665696000000L)
    }

    "be written with default implicit as '2011-12-03T10:15:30'" in {
      writes(date("2011-12-03")) aka "written date" must_== JsString(
        "2011-12-03")
    }

    "be written with custom pattern as '03/12/2011'" in {
      CustomWrites1.writes(date("2011-12-03")).
        aka("written date") must_== JsString("03/12/2011")
    }
  }

  "Instant" should {
    val DefaultWrites = implicitly[Writes[Instant]]
    import DefaultWrites.writes

    lazy val instant = Instant.parse("2011-12-03T10:15:30Z")

    @inline def format(pattern: String, temporal: Instant) =
      DateTimeFormatter.ofPattern(pattern).
        format(LocalDateTime.ofInstant(temporal, ZoneId.systemDefault))

    val customPattern1 = "dd/MM/yyyy, HH:mm:ss"
    val CustomWrites1 = Writes.temporalWrites[Instant, String](customPattern1)

    "be written as number" in {
      Writes.InstantNumberWrites.writes(Instant ofEpochMilli 1234567890L).
        aka("written date") must_== JsNumber(BigDecimal valueOf 1234567890L)
    }

    "be written with default implicit as '2011-12-03T10:15:30'" in {

      writes(instant) aka "written date" must_== JsString(
        format("yyyy-MM-dd'T'HH:mm:ss", instant))
    }

    "be written with custom pattern as '03/12/2011, 10:15:30'" in {
      CustomWrites1.writes(instant).
        aka("written date") must_== JsString(format(customPattern1, instant))
    }
  }
}
