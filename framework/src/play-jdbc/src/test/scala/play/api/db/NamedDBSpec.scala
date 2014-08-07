package play.api.db

import play.api.test._

import javax.inject.Inject
import javax.sql.DataSource

class NamedDBSpec extends PlaySpecification {

  "BoneCPModule" should {
    "bind datasources by name" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "db.default.driver" -> "org.h2.Driver",
        "db.default.url" -> "jdbc:h2:mem:default",
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      def checkURL(ds: DataSource, expected: String): Unit = {
        val connection = ds.getConnection
        try connection.getMetaData.getURL must_== expected
        finally connection.close()
      }

      val dbComponent = app.injector.instanceOf[DBComponent]
      checkURL(dbComponent.default, "jdbc:h2:mem:default")
      checkURL(dbComponent.other, "jdbc:h2:mem:other")
    }
  }

}

case class DBComponent @Inject() (
  @NamedDB("default") default: DataSource,
  @NamedDB("other") other: DataSource)
