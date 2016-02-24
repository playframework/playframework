/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

import java.math.BigDecimal
import java.time.{
  Clock,
  Instant,
  LocalDate,
  LocalDateTime,
  OffsetDateTime,
  ZoneId,
  ZonedDateTime,
  ZoneOffset
}
import java.time.format.DateTimeFormatter
import play.api.data.validation.ValidationError

object ReadsSpec extends org.specs2.mutable.Specification {

  title("JSON Reads")

  "Local date/time" should {
    val DefaultReads = implicitly[Reads[LocalDateTime]]
    import DefaultReads.reads

    val CustomReads1 = Reads.localDateTimeReads("dd/MM/yyyy, HH:mm:ss")

    @inline def dateTime(input: String) =
      LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)

    lazy val correctedReads = Reads.localDateTimeReads(
      DateTimeFormatter.ISO_DATE_TIME, _.drop(1))

    val CustomReads2 = Reads.localDateTimeReads(
      DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss"), _.drop(2))

    "be successfully read from number" in {
      reads(JsNumber(BigDecimal valueOf 123L)).
        aka("read date") must_== JsSuccess(LocalDateTime.ofInstant(
          Instant.ofEpochMilli(123L), ZoneOffset.UTC
        ))
    }

    "not be read from invalid string" in {
      reads(JsString("invalid")) aka "read date" must beLike {
        case JsError((_, ValidationError(
          "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
      }
    }

    "be successfully read with default implicit" >> {
      "from '2011-12-03T10:15:30'" in {
        reads(JsString("2011-12-03T10:15:30")).
          aka("read date") must_== JsSuccess(dateTime("2011-12-03T10:15:30"))
      }

      "from '2011-12-03T10:15:30+01:00' (with TZ offset)" in {
        reads(JsString("2011-12-03T10:15:30+01:00")) aka "read date" must_== (
          JsSuccess(dateTime("2011-12-03T10:15:30+01:00")))
      }

      "from '2011-12-03T10:15:30+01:00[Europe/Paris]' (with time zone)" in {
        reads(JsString("2011-12-03T10:15:30+01:00[Europe/Paris]")).
          aka("read date") must_== (
            JsSuccess(dateTime("2011-12-03T10:15:30+01:00[Europe/Paris]")))
      }
    }

    "be successfully read with custom pattern from '03/12/2011, 10:15:30'" in {
      CustomReads1.reads(JsString("03/12/2011, 10:15:30")).
        aka("read date") must_== JsSuccess(dateTime("2011-12-03T10:15:30"))
    }

    "not be read from invalid corrected string" >> {
      "with default implicit" in {
        correctedReads.reads(JsString("2011-12-03T10:15:30")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("03/12/2011, 10:15:30")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }
    }

    "be successfully read from corrected string" >> {
      lazy val time = dateTime("2011-12-03T10:15:30")

      "with default implicit" in {
        correctedReads.reads(JsString("_2011-12-03T10:15:30")).
          aka("read date") must_== JsSuccess(time)
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("# 03/12/2011, 10:15:30")).
          aka("read date") must_== JsSuccess(time)
      }
    }
  }

  "Offset date/time" should {
    val DefaultReads = implicitly[Reads[OffsetDateTime]]
    import DefaultReads.reads

    val CustomReads1 = Reads.offsetDateTimeReads("dd/MM/yyyy, HH:mm:ssXXX")

    @inline def dateTime(input: String) = OffsetDateTime.parse(input)

    lazy val correctedReads = Reads.offsetDateTimeReads(
      DateTimeFormatter.ISO_OFFSET_DATE_TIME, _.drop(1)
    )

    val CustomReads2 = Reads.offsetDateTimeReads(
      DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss ZZZ"), _.drop(2)
    )

    "not be read" >> {
      "from an invalid string" in {
        reads(JsString("invalid")) aka "read date" must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }

      "from a number" in {
        reads(JsNumber(123L)) aka "read date" must beLike {
          case JsError((_, ValidationError("error.expected.date" :: Nil) :: Nil) :: Nil) => ok
        }
      }
    }

    "be successfully read with default implicit" >> {
      "from '2011-12-03T10:15:30-05:00'" in {
        reads(JsString("2011-12-03T10:15:30-05:00")).
          aka("read date") must_== JsSuccess(dateTime("2011-12-03T10:15:30-05:00"))
      }
    }

    "be successfully read with custom pattern from '03/12/2011, 10:15:30'" in {
      CustomReads1.reads(JsString("03/12/2011, 10:15:30-05:00")).
        aka("read date") must_== JsSuccess(dateTime("2011-12-03T10:15:30-05:00"))
    }

    "not be read from invalid corrected string" >> {
      "with default implicit" in {
        correctedReads.reads(JsString("2011-12-03T10:15:30")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("03/12/2011, 10:15:30")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }
    }

    "be successfully read from corrected string" >> {
      lazy val time = dateTime("2011-12-03T10:15:30-05:00")

      "with default implicit" in {
        correctedReads.reads(JsString("_2011-12-03T10:15:30-05:00")).
          aka("read date") must_== JsSuccess(time)
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("# 03/12/2011, 10:15:30 -0500")).
          aka("read date") must_== JsSuccess(time)
      }
    }
  }

  "Zoned date/time" should {
    val DefaultReads = implicitly[Reads[ZonedDateTime]]
    import DefaultReads.reads

    val CustomReads1 = Reads.zonedDateTimeReads("dd/MM/yyyy, HH:mm:ssXXX")

    @inline def dateTime(input: String) = ZonedDateTime.parse(input)

    lazy val correctedReads = Reads.zonedDateTimeReads(
      DateTimeFormatter.ISO_DATE_TIME, _.drop(1))

    val CustomReads2 = Reads.zonedDateTimeReads(
      DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ssVV"), _.drop(2)
    )

    "be successfully read from number" in {
      reads(JsNumber(BigDecimal valueOf 123L)).
        aka("read date") must_== JsSuccess(ZonedDateTime.ofInstant(
          Instant.ofEpochMilli(123L), ZoneOffset.UTC
        ))
    }

    "not be read from invalid string" in {
      reads(JsString("invalid")) aka "read date" must beLike {
        case JsError((_, ValidationError(
          "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
      }
    }

    "be successfully read with default implicit" >> {
      "from '2011-12-03T10:15:30+01:00' (with TZ offset)" in {
        reads(JsString("2011-12-03T10:15:30+01:00")) aka "read date" must_== (
          JsSuccess(dateTime("2011-12-03T10:15:30+01:00")))
      }

      "from '2011-12-03T10:15:30+01:00[Europe/Paris]' (with time zone)" in {
        reads(JsString("2011-12-03T10:15:30+01:00[Europe/Paris]")).
          aka("read date") must_== (
            JsSuccess(dateTime("2011-12-03T10:15:30+01:00[Europe/Paris]")))
      }
    }

    "be successfully read with custom pattern from '03/12/2011, 10:15:30+08:00'" in {
      CustomReads1.reads(JsString("03/12/2011, 10:15:30+08:00")).
        aka("read date") must_== JsSuccess(dateTime("2011-12-03T10:15:30+08:00"))
    }

    "not be read from invalid corrected string" >> {
      "with default implicit" in {
        correctedReads.reads(JsString("2011-12-03T10:15:30")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("03/12/2011, 10:15:30+08:00")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }
    }

    "be successfully read from corrected string" >> {
      lazy val time = dateTime("2011-12-03T10:15:30+08:00")

      "with default implicit" in {
        correctedReads.reads(JsString("_2011-12-03T10:15:30+08:00")).
          aka("read date") must_== JsSuccess(time)
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("# 03/12/2011, 10:15:30+08:00")).
          aka("read date") must_== JsSuccess(time)
      }
    }
  }

  "Local date" should {
    val DefaultReads = implicitly[Reads[LocalDate]]
    import DefaultReads.reads

    val CustomReads1 = Reads.localDateReads("dd/MM/yyyy")

    @inline def date(input: String) = LocalDate.parse(input)

    lazy val correctedReads = Reads.localDateReads(
      DateTimeFormatter.ISO_DATE, _.drop(1))

    val CustomReads2 = Reads.localDateReads(
      DateTimeFormatter.ofPattern("dd/MM/yyyy"), _.drop(2))

    "be successfully read from number" in {
      val beforeMidnight = Instant.parse("1970-01-01T23:55:00Z")
      val d = LocalDate.parse("1970-01-01")

      reads(JsNumber(BigDecimal valueOf beforeMidnight.toEpochMilli)).
        aka("read date") must_== JsSuccess(d)
    }

    "not be read from invalid string" in {
      reads(JsString("invalid")) aka "read date" must beLike {
        case JsError((_, ValidationError(
          "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
      }
    }

    "be successfully read with default implicit from '2011-12-03'" in {
      reads(JsString("2011-12-03")).
        aka("read date") must_== JsSuccess(date("2011-12-03"))
    }

    "be successfully read with custom pattern from '03/12/2011'" in {
      CustomReads1.reads(JsString("03/12/2011")).
        aka("read date") must_== JsSuccess(date("2011-12-03"))
    }

    "not be read from invalid corrected string" >> {
      "with default implicit" in {
        correctedReads.reads(JsString("2011-12-03")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("03/12/2011")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }
    }

    "be successfully read from corrected string" >> {
      lazy val d = date("2011-12-03")

      "with default implicit" in {
        correctedReads.reads(JsString("_2011-12-03")).
          aka("read date") must_== JsSuccess(d)
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("# 03/12/2011")).
          aka("read date") must_== JsSuccess(d)
      }
    }
  }

  "Instant" should {
    val DefaultReads = implicitly[Reads[Instant]]
    import DefaultReads.reads

    val CustomReads1 = Reads.instantReads("dd/MM/yyyy, HH:mm:ss X")

    lazy val correctedReads = Reads.instantReads(
      DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC), _.drop(1)
    )

    val CustomReads2 = Reads.instantReads(
      DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss").withZone(ZoneOffset.UTC), _.drop(2)
    )

    "be successfully read from number" in {
      reads(JsNumber(BigDecimal valueOf 123L)).
        aka("read date") must_== JsSuccess(Instant ofEpochMilli 123L)
    }

    "not be read from invalid string" in {
      reads(JsString("invalid")) aka "read date" must beLike {
        case JsError((_, ValidationError(
          "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
      }
    }

    "be successfully read with default implicit" >> {
      "from '2015-05-01T13:00:00Z' (with zeros)" in {
        reads(JsString("2015-05-01T00:00:00Z")).
          aka("read data") must_== JsSuccess(Instant.parse("2015-05-01T00:00:00Z"))
      }

      "from '2011-12-03T10:15:30Z'" in {
        reads(JsString("2011-12-03T10:15:30Z")).
          aka("read date") must_== JsSuccess(Instant.parse("2011-12-03T10:15:30Z"))
      }

      "from '2015-05-01T13:00:00+02:00' (with TZ offset and zeros)" in {
        reads(JsString("2015-05-01T13:00:00+02:00")) must_== JsSuccess(
          Instant.parse("2015-05-01T11:00:00Z")
        )
      }

      "from '2011-12-03T10:15:30+01:00' (with TZ offset)" in {
        reads(JsString("2011-12-03T10:15:30+01:00")) aka "read date" must_== (
          JsSuccess(Instant.parse("2011-12-03T09:15:30Z"))
        )
      }

      "from '2011-12-03T10:15:30+01:00[Europe/Paris]' (with time zone)" in {
        reads(JsString("2011-12-03T10:15:30+01:00[Europe/Paris]")).
          aka("read date") must_== (
            JsSuccess(Instant.parse("2011-12-03T09:15:30Z"))
          )
      }

      "from '2011-12-03T00:00:00+01:00[Europe/Paris]' (with time zone)" in {
        reads(JsString("2011-12-03T00:00:00+01:00[Europe/Paris]")).
          aka("read date") must_== (
            JsSuccess(Instant.parse("2011-12-02T23:00:00Z"))
          )
      }
    }

    "be successfully read with custom pattern from '03/12/2011, 10:15:30 Z'" in {
      CustomReads1.reads(JsString("03/12/2011, 10:15:30 Z")).
        aka("read date") must_== JsSuccess(Instant.parse("2011-12-03T10:15:30Z"))
    }

    "not be read from invalid corrected string" >> {
      "with default implicit" in {
        correctedReads.reads(JsString("2011-12-03T10:15:30")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("03/12/2011, 10:15:30")) must beLike {
          case JsError((_, ValidationError(
            "error.expected.date.isoformat" :: Nil, _) :: Nil) :: Nil) => ok
        }
      }
    }

    "be successfully read from corrected string" >> {
      lazy val time = Instant.parse("2011-12-03T10:15:30Z")

      "with default implicit" in {
        correctedReads.reads(JsString("_2011-12-03T10:15:30")).
          aka("read date") must_== JsSuccess(time)
      }

      "with custom formatter" in {
        CustomReads2.reads(JsString("# 03/12/2011, 10:15:30")).
          aka("read date") must_== JsSuccess(time)
      }
    }
  }

  "Reads flatMap" should {
    "not repath the second result" >> {
      val aPath = JsPath \ "a"
      val readsA: Reads[String] = aPath.read[String]
      val value = "string"
      val aJson = aPath.write[String].writes(value)

      "in case of success" in {
        val flatMappedReads = readsA.flatMap(_ => readsA)
        aJson.validate(flatMappedReads).aka("read a").must_==(JsSuccess(value, aPath))
      }
      "in case of failure" in {
        val readsAFail = aPath.read[Int]
        val flatMappedReads = readsA.flatMap(_ => readsAFail)
        aJson.validate(flatMappedReads).aka("read a")
          .must_==(JsError(List((aPath, List(ValidationError("error.expected.jsnumber"))))))
      }
    }
  }
}
