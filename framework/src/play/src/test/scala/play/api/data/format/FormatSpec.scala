package play.api.data.format

import org.specs2.mutable.Specification
import java.util.{Date, TimeZone}

object FormatSpec extends Specification {
  "dateFormat" should {
    "support custom time zones" << {
      val data = Map("date" -> "00:00")

      val format = Formats.dateFormat("HH:mm", TimeZone.getTimeZone("America/Los_Angeles"))
      format.bind("date", data).right.map(_.getTime) should beRight (28800000L)
      format.unbind("date", new Date(28800000L)) should equalTo (data)

      val format2 = Formats.dateFormat("HH:mm", TimeZone.getTimeZone("GMT+0000"))
      format2.bind("date", data).right.map(_.getTime) should beRight (0L)
      format2.unbind("date", new Date(0L)) should equalTo (data)
    }
  }
}
