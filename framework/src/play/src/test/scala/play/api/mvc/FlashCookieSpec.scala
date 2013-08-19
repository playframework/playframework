package play.api.mvc

import org.specs2.mutable._
import java.net.URLEncoder

object FlashCookieSpec extends Specification {

  def oldEncoder(data: Map[String, String]): String = {
    URLEncoder.encode(
      data.map(d => d._1 + ":" + d._2).mkString("\u0000"),
      "UTF-8"
    )
  }

  "Flash cookies" should {
    "bake in a header and value" in {
      val es = Flash.encode(Map("a" -> "b"))
      val m = Flash.decode(es)
      m.size must_== 1
      m("a") must_== "b"
    }
    "bake in multiple headers and values" in {
      val es = Flash.encode(Map("a" -> "b", "c" -> "d"))
      val m = Flash.decode(es)
      m.size must_== 2
      m("a") must_== "b"
      m("c") must_== "d"
    }
    "bake in a header an empty value" in {
      val es = Flash.encode(Map("a" -> ""))
      val m = Flash.decode(es)
      m.size must_== 1
      m("a") must_== ""
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
    "put disallows null values" in {
      val c = Flash(Map("foo" -> "bar"))
      c + (("x", null)) must throwA(new IllegalArgumentException("requirement failed: Cookie values cannot be null"))
    }
  }
}
