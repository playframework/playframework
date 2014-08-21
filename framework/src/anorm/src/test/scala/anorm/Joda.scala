package anorm

import java.sql.{ Date, Timestamp }

import org.joda.time.{ DateTime, Instant }

import org.specs2.mutable.Specification

import acolyte.jdbc.RowLists.{ dateList, longList, timestampList }
import acolyte.jdbc.AcolyteDSL.withQueryResult
import acolyte.jdbc.Implicits._

import SqlParser.scalar

trait JodaColumnSpec { specs: Specification =>
  "Column mapped as joda-time instant" should {
    val time = Instant.now()

    "be parsed from date" in withQueryResult(
      dateList :+ new Date(time.getMillis)) { implicit con =>
        SQL("SELECT d").as(scalar[Instant].single).
          aka("parsed date") must_== time
      }

    "be parsed from timestamp" in withQueryResult(
      timestampList :+ new Timestamp(time.getMillis)) { implicit con =>
        SQL("SELECT ts").as(scalar[Instant].single).
          aka("parsed date") must beLike {
            case d => d aka "time" must_== time
          }
      }

    "be parsed from time" in withQueryResult(longList :+ time.getMillis) {
      implicit con =>
        SQL("SELECT time").as(scalar[Instant].single).
          aka("parsed date") must_== time
    }
  }
}

trait JodaParameterSpec { specs: ParameterSpec.type =>
  lazy val dateTime1 = new DateTime(Date1.getTime)
  lazy val instant1 = new Instant(Date1.getTime)

  "Named parameters" should {
    "be joda-time datetime" in withConnection() { implicit c =>
      SQL("set-date {p}").on("p" -> dateTime1).execute() must beFalse
    }

    "be null joda-time datetime" in withConnection() { implicit c =>
      SQL("set-null-date {p}").
        on("p" -> null.asInstanceOf[DateTime]).execute() must beFalse
    }

    "be joda-time instant" in withConnection() { implicit c =>
      SQL("set-date {p}").on("p" -> instant1).execute() must beFalse
    }

    "be null joda-time instant" in withConnection() { implicit c =>
      SQL("set-null-date {p}").
        on("p" -> null.asInstanceOf[Instant]).execute() must beFalse
    }
  }
}
