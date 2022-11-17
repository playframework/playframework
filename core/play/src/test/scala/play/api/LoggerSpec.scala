/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.slf4j.Marker
import org.slf4j.MarkerFactory
import org.specs2.mutable.Specification

class LoggerSpec extends Specification {
  "MarkerContext.apply" should {
    "return some marker" in {
      val marker = MarkerFactory.getMarker("SOMEMARKER")
      val mc     = MarkerContext(marker)
      mc.marker must beSome(marker)
    }

    "return a MarkerContext with None if passed null" in {
      val mc = MarkerContext(null)
      mc.marker must beNone
    }
  }

  "MarkerContext" should {
    "implicitly convert a Marker to a MarkerContext" in {
      val marker: Marker             = MarkerFactory.getMarker("SOMEMARKER")
      implicit val mc: MarkerContext = marker

      mc.marker must beSome(marker)
    }
  }

  "DefaultMarkerContext" should {
    "define a case object" in {
      val marker = MarkerFactory.getMarker("SOMEMARKER")
      case object SomeMarkerContext extends DefaultMarkerContext(marker)
      SomeMarkerContext.marker must beSome(marker)
    }
  }
}
