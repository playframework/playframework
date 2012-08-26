import anorm._
import org.specs2.mutable._
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test.FakeApplication
import play.api.Play.current

class EvolutionsSpec extends Specification {

  "Evolutions" should {
    "be run against the configured database in fake application when testing" in {
      running(FakeApplication(additionalConfiguration = inMemoryDatabase("mock"))) {
        evolutionFor("mock")
        DB.withConnection("mock") { implicit conn =>
          SQL("insert into Mock (value) values ('foobar')").executeUpdate()
          val first = SQL("select * from Mock where value = 'foobar'").apply().head
          first[String]("value") must_== "foobar"
          first[Int]("id") must_!= null
        }
      }
    }
  }
}
