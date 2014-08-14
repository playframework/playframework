package play.api.db

import java.sql.{ DriverManager, SQLException }
import org.specs2.mutable.Specification
import play.api.{ Configuration, Mode }
import scala.util.Try

object BoneCPSpec extends Specification {
  "BoneCP DB API" title

  "JDBC driver" should {
    sequential

    "be registered for H2 before BoneCP starts" in {
      DriverManager.getDriver("jdbc:h2:mem:") aka "H2 driver" must not(beNull)
    }

    "not be registered for Acolyte until BoneCP is connected" in {
      Try { // Ensure driver is not registered
        DriverManager.deregisterDriver(DriverManager.getDriver(jdbcUrl))
      }

      DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must(
        throwA[SQLException](message = "No suitable driver"))
    }

    "be registered for both Acolyte & H2 when BoneCP is connected" in {
      bonecp.connect()

      (DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must not(beNull)).
        and(DriverManager.getDriver("jdbc:h2:mem:").
          aka("H2 driver") must not(beNull))
    }

    "be deregistered for Acolyte but still there for H2 after BoneCP stops" in {
      bonecp.shutdown()

      (DriverManager.getDriver("jdbc:h2:mem:") aka "H2 driver" must not(beNull))
      .and(DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must {
        throwA[SQLException](message = "No suitable driver")
      })
    }
  }

  val jdbcUrl = "jdbc:acolyte:test?handler=BoneCPSpec"

  lazy val bonecp: BoneConnectionPool = {
    // Fake driver
    acolyte.jdbc.Driver.register("BoneCPSpec", acolyte.jdbc.CompositeHandler.empty())

    new BoneConnectionPool(
      Configuration.from(Map(
        "default.driver" -> "acolyte.jdbc.Driver",
        "default.url" -> jdbcUrl)),
      this.getClass.getClassLoader
    )
  }
}
