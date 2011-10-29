package play.api.cache

import org.specs2.mutable._

object CacheSpec extends Specification {

  "A cache" should {

    "get/set items the scala way" in {
      val c = new Cache
      c.set("peter", "soooo")
      c.get[String]("peter").get must equalTo("soooo")
    }

  }

}
