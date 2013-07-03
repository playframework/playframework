package play.api.db.slick.test

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.db._
import play.api.Play.current
import play.api.db.slick.DB

object Q extends Table[(Int, String, Int)]("a") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  def a = column[String]("a", O.NotNull)
  def tId = column[Int]("t_id", O.NotNull)
  def t = foreignKey("t_fk", tId, T)(_.id)
  def * = id ~ a ~ tId
}

object T extends Table[(Int, String, Int)]("b") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  def b = column[String]("b", O.NotNull)
  def uId = column[Int]("u_id", O.NotNull)
  def u = foreignKey("u_fk", uId, U)(_.id)
  def * = id ~ b ~ uId
}

object U extends Table[(Int, String)]("c") {
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  def c = column[String]("c", O.NotNull)
  def * = id ~ c
}

class SlickDDLPluginSpec extends Specification {

  def testConfiguration = {
    inMemoryDatabase() ++ Map("slick.default" -> "play.api.db.slick.test.*")
  }

  "SlickDDLPlugin" should {

    "create working evolution" in {
      try {
        running(FakeApplication(additionalConfiguration = testConfiguration)) {
          DB.withSession { implicit session =>
            val uId = U.insert((1, "q"))
            val tId = T.insert((1, "t", uId))
            val qId = Q.insert((1, "q", tId))

            Query(Q).list must equalTo(Seq((1, "q", tId)))
          }
        }
      } finally {
        // evolutions cleanup
        scalax.file.Path("conf").deleteRecursively()
      }
    }
  }
}
