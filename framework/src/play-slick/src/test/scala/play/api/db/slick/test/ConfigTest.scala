package play.api.db.slick.test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.db._
import play.api.Play.current
import play.api.db.slick.Config
import play.api.db.slick.DB


class ConfigSpec extends Specification {

  def testConfiguration = {
    Map(
      "db.somedb.driver" -> "org.h2.Driver",
      "db.default.driver" -> "com.mysql.jdbc.Driver",
      "evolutionplugin" -> "disabled")
  }

  def fakeApplication = FakeApplication(
    withoutPlugins = Seq("play.api.db.BoneCPPlugin"),
    additionalConfiguration = testConfiguration)

  "Config.driver" should {
    "return the driver for the given database" in {
      running(fakeApplication) {
        val driver = Config.driver(play.api.Play.current, "somedb")
        driver must equalTo(scala.slick.driver.H2Driver)
      }
    }

    "return the driver for the default database when db name is not specified" in {
      running(fakeApplication) {
        val driver = Config.driver(play.api.Play.current)
        driver must equalTo(scala.slick.driver.MySQLDriver)
      }
    }
  }

  "DB.driver" should {
    "return the driver for the given database" in {
      running(fakeApplication) {
        val db = DB("somedb")
        val driver = db.driver(play.api.Play.current)
        driver must equalTo(scala.slick.driver.H2Driver)
      }
    }

    "return the driver for the default database when db name is not specified" in {
      running(fakeApplication) {
        val driver = DB.driver(play.api.Play.current)
        driver must equalTo(scala.slick.driver.MySQLDriver)
      }
    }
  }
}
