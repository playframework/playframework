/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.data.format

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import org.specs2.mutable.Specification

class PlayDateSpec extends Specification {
  "PlayDate.toZonedDateTime(ZoneId)" should {
    "return a valid date" in {
      val date1 = PlayDate.parse("2016 16:01", DateTimeFormatter.ofPattern("yyyy HH:mm"))

      date1.toZonedDateTime(ZoneOffset.UTC).getHour must_=== 16
      date1.toZonedDateTime(ZoneOffset.UTC).getYear must_=== 2016

      val date2 =
        PlayDate.parse("2019-08-03T17:01:49.123+02:00", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))

      date2.toZonedDateTime(ZoneOffset.UTC).getYear must_=== 2019
      date2.toZonedDateTime(ZoneOffset.UTC).getHour must_=== 15
      date2.toZonedDateTime(ZoneOffset.UTC).getSecond must_=== 49
      date2.toZonedDateTime(ZoneOffset.UTC).getNano must_=== 123000000
    }
  }
}
