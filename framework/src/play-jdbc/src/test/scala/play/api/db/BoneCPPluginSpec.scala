package play.api.db

import scala.util.Try
import java.sql.{ DriverManager, SQLException }
import play.api.test.FakeApplication
import org.specs2.mutable.Specification

object BoneCPPluginSpec extends Specification {
  "BoneCP DB plugin" title

  "JDBC driver" should {
    sequential 

    "be registered for H2 before plugin starts" in {
      DriverManager.getDriver("jdbc:h2:mem:") aka "H2 driver" must not(beNull)
    }

    "not be registered for Acolyte until plugin is started" in {
      Try { // Ensure driver is not registered
        DriverManager.deregisterDriver(DriverManager.getDriver(jdbcUrl))
      }

      DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must(
        throwA[SQLException](message = "No suitable driver"))
    }

    "be registered for both Acolyte & H2 when plugin is started" in {
      plugin.onStart()

      (DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must not(beNull)).
        and(DriverManager.getDriver("jdbc:h2:mem:").
          aka("H2 driver") must not(beNull))
    }

    "be deregistered for Acolyte but still there for H2 after plugin stops" in {
      plugin.onStop()

      (DriverManager.getDriver("jdbc:h2:mem:") aka "H2 driver" must not(beNull))
      .and(DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must {
        throwA[SQLException](message = "No suitable driver")
      })
    }
  }

  val jdbcUrl = "jdbc:acolyte:test?handler=boneCPPluginSpec"
  lazy val plugin = {
    acolyte.Driver.register("boneCPPluginSpec", 
      acolyte.CompositeHandler.empty()) // Fake driver

    new BoneCPPlugin(
      FakeApplication(additionalConfiguration = Map(
        "db.default.driver" -> "acolyte.Driver",
        "db.default.url" -> jdbcUrl)))
  }
}
