package play.api.db

import java.io.File
import java.sql.{ DriverManager, SQLException }
import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment, Mode }
import play.api.inject.DefaultApplicationLifecycle
import play.api.test._
import scala.util.Try

object BoneCPApiSpec extends Specification with DefaultAwaitTimeout with FutureAwaits {
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
      dbApi // start

      (DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must not(beNull)).
        and(DriverManager.getDriver("jdbc:h2:mem:").
          aka("H2 driver") must not(beNull))
    }

    "be deregistered for Acolyte but still there for H2 after plugin stops" in {
      await(lifecycle.stop())

      (DriverManager.getDriver("jdbc:h2:mem:") aka "H2 driver" must not(beNull))
      .and(DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must {
        throwA[SQLException](message = "No suitable driver")
      })
    }
  }

  val jdbcUrl = "jdbc:acolyte:test?handler=boneCPPluginSpec"

  lazy val lifecycle = new DefaultApplicationLifecycle

  lazy val dbApi = {
    acolyte.jdbc.Driver.register("boneCPPluginSpec",
      acolyte.jdbc.CompositeHandler.empty()) // Fake driver

    val dbConfig = new DefaultDBConfig(Configuration.from(Map(
      "play.modules.db.bonecp.enabled" -> true,
      "db.default.driver" -> "acolyte.jdbc.Driver",
      "db.default.url" -> jdbcUrl))
    ).get

    val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Dev)

    new BoneCPApi(dbConfig, environment, lifecycle)
  }
}
