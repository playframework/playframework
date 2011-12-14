package play.api.cache

import org.specs2.mutable._

object CacheSpec extends Specification {

  "A cache" should {

    "get/set items the scala way" in {
      val c = new BasicCache
      c.set("peter", "soooo")
      c.get[String]("badkey").isDefined must equalTo(false)
      c.get[Int]("peter").isDefined must equalTo(false)
      c.get[String]("peter").get must equalTo("soooo")
    }
    "get multiple items the scala way" in {
      val c = new BasicCache
      c.set("peter", "soooo")
      c.set("john", "soooo")
      val res: Map[String, Option[String]] = c.get[String]("peter", "john", "jo")
      res.size must equalTo(3)
      res("jo").isDefined must equalTo(false)
      c.set("foo", 1)
      c.set("foo2", 2)
      val res2: Map[String, Option[Int]] = c.get[Int]("peter", "foo2", "foo")
      res2("peter").isDefined must equalTo(false)
      val res3: Map[String, Option[Any]] = c.get[Any]("peter", "foo2", "foo")
      res3("peter").isDefined must equalTo(true)
    }
    "set items with expiry" in {
      val c = new BasicCache
      c.set("peter", "soooo")
      c.get[String]("peter").get must equalTo("soooo")
      c.set("p", "hm", 1)
      Thread.sleep(1200)
      c.get[String]("p").isDefined must equalTo(false)
    }
    "delete item" in {
      val c = new BasicCache
      c.set("peter", null)
      c.get[String]("peter").isDefined must equalTo(false)
    }

  }
}
