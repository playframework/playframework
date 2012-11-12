import anorm._
import org.specs2.mutable._
import play.api.db.DB
import play.api.db.evolutions.Evolutions
import play.api.test.Helpers._
import play.api.test.{WithApplication, FakeApplication}

class EvolutionsSpec extends Specification {

  "Evolutions" should {
    "be run against the configured database in fake application when testing" in new WithApplication(
      FakeApplication(additionalConfiguration = inMemoryDatabase("mock"))
    ) {
      Evolutions.applyFor("mock")
      DB.withConnection("mock") { implicit conn =>
        SQL("insert into Mock (value) values ('foobar')").executeUpdate()
        val first = SQL("select * from Mock where value = 'foobar'").apply().head
        first[String]("value") must_== "foobar"
        first[Int]("id") must_!= null
      }
    }
  }
}
