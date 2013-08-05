package play.api.mvc

import org.specs2.mutable._

object FlashCookieSpec extends Specification {

  "Flash cookies" should {
    "bake in a header and value" in {
      val es = Flash.encode(Map("a" -> "b"))
      val m = Flash.decode(es)
      m.size must_== 1
      m("a") must_== "b"
    }
    "encode values such that no extra keys can be created" in {
      val es = Flash.encode(Map("a" -> "b&c=d"))
      val m = Flash.decode(es)
      m.size must_== 1
      m("a") must_== "b&c=d"
    }
    "specifically exclude control chars" in {
      for (i <- 0 until 32) {
        val s = Character.toChars(i).toString
        val es = Flash.encode(Map("a" -> s))
        es must not contain s

        val m = Flash.decode(es)
        m.size must_== 1
        m("a") must_== s
      }
      success
    }
    "specifically exclude special cookie chars" in {
      val es = Flash.encode(Map("a" -> " \",;\\"))
      es must not contain " "
      es must not contain "\""
      es must not contain ","
      es must not contain ";"
      es must not contain "\\"

      val m = Flash.decode(es)
      m.size must_== 1
      m("a") must_== " \",;\\"
    }
  }
}
