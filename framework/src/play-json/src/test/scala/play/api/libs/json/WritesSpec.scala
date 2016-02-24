/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

import java.time.{
  Instant,
  LocalDateTime,
  LocalDate,
  OffsetDateTime,
  ZonedDateTime,
  ZoneOffset
}
import java.time.format.DateTimeFormatter

object WritesSpec extends org.specs2.mutable.Specification {

  title("JSON Writes")

  "Local date/time" should {
    val DefaultWrites = implicitly[Writes[LocalDateTime]]
    import DefaultWrites.writes

    @inline def dateTime(input: String) = LocalDateTime.parse(input)

    val CustomWrites1 = Writes.
      temporalWrites[LocalDateTime, String]("dd/MM/yyyy, HH:mm:ss")

    "be written as number" in {
      Writes.LocalDateTimeNumberWrites.writes(LocalDateTime.ofInstant(
        Instant.ofEpochMilli(1234567890L), ZoneOffset.UTC
      )).
        aka("written date") must_== JsNumber(BigDecimal valueOf 1234567890L)
    }

    "be written with default implicit as '2011-12-03T10:15:30'" in {
      writes(dateTime("2011-12-03T10:15:30")) aka "written date" must_== (
        JsString("2011-12-03T10:15:30")
      )
    }

    "be written with custom pattern as '03/12/2011, 10:15:30'" in {
      CustomWrites1.writes(dateTime("2011-12-03T10:15:30")).
        aka("written date") must_== JsString("03/12/2011, 10:15:30")
    }
  }

  "Offset date/time" should {
    val DefaultWrites = implicitly[Writes[OffsetDateTime]]
    import DefaultWrites.writes

    val CustomWrites1 = Writes.
      temporalWrites[OffsetDateTime, String]("dd/MM/yyyy, HH:mm:ss (XXX)")

    "be written with default implicit as '2011-12-03T10:15:30-01:30'" in {
      writes(OffsetDateTime.parse("2011-12-03T10:15:30-01:30")) aka "written date" must_== (
        JsString("2011-12-03T10:15:30-01:30")
      )
    }

    "be written with custom pattern as '03/12/2011, 10:15:30 (-01:30)'" in {
      CustomWrites1.writes(OffsetDateTime.parse("2011-12-03T10:15:30-01:30")).
        aka("written date") must_== JsString("03/12/2011, 10:15:30 (-01:30)")
    }
  }

  "Zoned date/time" should {
    val DefaultWrites = implicitly[Writes[ZonedDateTime]]
    import DefaultWrites.writes

    @inline def dateTime(input: String) = ZonedDateTime.parse(input)

    val CustomWrites1 = Writes.
      temporalWrites[ZonedDateTime, String]("dd/MM/yyyy, HH:mm:ss")

    "be written as number" in {
      Writes.ZonedDateTimeNumberWrites.writes(ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(1234567890L), ZoneOffset.UTC
      )).
        aka("written date") must_== JsNumber(BigDecimal valueOf 1234567890L)
    }

    "be written with default implicit as '2011-12-03T10:15:30+01:00[Europe/Paris]'" in {
      writes(dateTime("2011-12-03T10:15:30+01:00[Europe/Paris]")) aka "written date" must_== (
        JsString("2011-12-03T10:15:30+01:00[Europe/Paris]")
      )
    }

    "be written with default implicit as '2011-12-03T10:15:30+06:30'" in {
      writes(dateTime("2011-12-03T10:15:30+06:30")) aka "written date" must_== (
        JsString("2011-12-03T10:15:30+06:30")
      )
    }

    "be written with custom pattern as '03/12/2011, 10:15:30'" in {
      CustomWrites1.writes(dateTime("2011-12-03T10:15:30+05:30")).
        aka("written date") must_== JsString("03/12/2011, 10:15:30")
    }
  }

  "Local date" should {
    val DefaultWrites = implicitly[Writes[LocalDate]]
    import DefaultWrites.writes

    @inline def date(input: String) = LocalDate.parse(input)

    val CustomWrites1 = Writes.temporalWrites[LocalDate, String]("dd/MM/yyyy")

    "be written as number" in {
      Writes.LocalDateNumberWrites.writes(
        LocalDate ofEpochDay 1234567890L) aka "written date" must_== JsNumber(
          BigDecimal valueOf 106666665696000000L)
    }

    "be written with default implicit as '2011-12-03'" in {
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

    val customPattern1 = "dd/MM/yyyy, HH:mm:ss"
    val CustomWrites1 = Writes.temporalWrites[Instant, String](customPattern1)

    "be written as number" in {
      Writes.InstantNumberWrites.writes(Instant ofEpochMilli 1234567890L).
        aka("written date") must_== JsNumber(BigDecimal valueOf 1234567890L)
    }

    "be written with default implicit as '2011-12-03T10:15:30Z'" in {
      writes(instant) aka "written date" must_== JsString("2011-12-03T10:15:30Z")
    }

    "be written with custom pattern as '03/12/2011, 10:15:30'" in {
      CustomWrites1.writes(instant).
        aka("written date") must_== JsString("03/12/2011, 10:15:30")
    }
  }

  "OWrites" should {
    val writes = OWrites[Foo] { foo =>
      Json.obj("bar" -> foo.bar)
    }
    val time = System.currentTimeMillis()

    "be transformed with JsObject function" in {
      val transformed: OWrites[Foo] = writes.transform({ obj: JsObject =>
        obj ++ Json.obj("time" -> time)
      })
      val written: JsObject = transformed.writes(Foo("Lorem"))

      written must_== Json.obj("bar" -> "Lorem", "time" -> time)
    }

    "be transformed with another OWrites" in {
      val transformed: OWrites[Foo] =
        writes.transform(OWrites[JsObject] { obj =>
          obj ++ Json.obj("time" -> time)
        })
      val written: JsObject = transformed.writes(Foo("Lorem"))

      written must_== Json.obj("bar" -> "Lorem", "time" -> time)
    }
  }

  // ---

  case class Foo(bar: String)
}
