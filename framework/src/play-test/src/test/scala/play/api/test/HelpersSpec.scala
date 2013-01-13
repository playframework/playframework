package play.api.test

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._

class HelpersSpec extends Specification {

  "inMemoryDatabase" should {

    "change database with a name argument" in {
      val inMemoryDatabaseConfiguration = inMemoryDatabase("test")
      inMemoryDatabaseConfiguration.get("db.test.driver") must beSome("org.h2.Driver")
      inMemoryDatabaseConfiguration.get("db.test.url") must beSome.which { url =>
        url.startsWith("jdbc:h2:mem:play-test-")
      }
    }

    "add options" in {
      val inMemoryDatabaseConfiguration = inMemoryDatabase("test", Map("MODE" -> "PostgreSQL", "DB_CLOSE_DELAY" -> "-1"))
      inMemoryDatabaseConfiguration.get("db.test.driver") must beSome("org.h2.Driver")
      inMemoryDatabaseConfiguration.get("db.test.url") must beSome.which { url =>
        """^jdbc:h2:mem:play-test([0-9-]+);MODE=PostgreSQL;DB_CLOSE_DELAY=-1$""".r.findFirstIn(url).isDefined
      }
    }
  }

}
