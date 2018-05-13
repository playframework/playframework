/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.specs2.mutable.Specification

class ModeSpec extends Specification {

  "Scala Mode" should {
    "convert Dev mode to Java play.Mode.DEV" in {
      Mode.Dev.asJava must beEqualTo(play.Mode.DEV)
    }
    "convert Test mode to Java play.Mode.TEST" in {
      Mode.Test.asJava must beEqualTo(play.Mode.TEST)
    }
    "convert Prod mode to Java play.Mode.PROD" in {
      Mode.Prod.asJava must beEqualTo(play.Mode.PROD)
    }
  }

  "Java Mode" should {
    "convert play.Mode.DEV to Scala Dev" in {
      play.Mode.DEV.asScala() must beEqualTo(Mode.Dev)
    }
    "convert play.Mode.TEST to Scala Test" in {
      play.Mode.TEST.asScala() must beEqualTo(Mode.Test)
    }
    "convert play.Mode.PROD to Scala Prod" in {
      play.Mode.PROD.asScala() must beEqualTo(Mode.Prod)
    }
  }
}
