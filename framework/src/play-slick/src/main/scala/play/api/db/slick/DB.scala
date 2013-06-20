package play.api.db.slick

import play.api.Plugin
import play.api.Application
import scala.slick.session.Session
import scala.slick.session.Database
import play.api.db.{ DB => PlayDB }
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper object to access Databases using Slick
 */
object DB extends DB {
  def apply(name: String) = {
    new DB {
      override lazy val CurrentDB = name
    }
  }
}

trait DB {
  import play.api.db.{ DB => PlayDB }

  def driver(implicit app: Application) = Config.driver(app, CurrentDB)

  def database(name: String)(implicit app: Application): Database = {
    if (app.configuration.getConfig(s"db.$name").isEmpty) app.configuration.reportError(s"db.$name", s"While loading datasource: could not find db.$name in configuration", None)
    val db = Database.forDataSource(PlayDB.getDataSource(name)(app))
    if (db == null) throw app.configuration.reportError(s"db.$name", s"While loading datasource: could not create database named: $name", None)
    db
  }

  def withSession[A](name: String)(block: Session => A)(implicit app: Application): A = {

    database(name).withSession { session: Session =>
      block(session)
    }
  }

  def withTransaction[A](name: String)(block: Session => A)(implicit app: Application): A = {
    database(name).withTransaction { session: Session =>
      block(session)
    }
  }

  protected lazy val CurrentDB = "default"

  def database(implicit app: Application): Database = {
    database(CurrentDB)(app)
  }

  def withSession[A](block: Session => A)(implicit app: Application): A = {
    database.withSession { session: Session =>
      block(session)
    }
  }

  def withTransaction[A](block: Session => A)(implicit app: Application): A = {
    database.withTransaction { session: Session =>
      block(session)
    }
  }

}
