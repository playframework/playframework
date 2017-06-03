package play.api.db

import javax.sql.DataSource

import com.typesafe.config.Config
import play.api.inject.Injector
import play.api.{ Configuration, Environment }
import play.utils.Reflect

trait AsyncConnectionPool {
  top =>

  /**
   * Create a data source with the given configuration.
   *
   * @param name the database name
   * @param configuration the data source configuration
   * @return a data source backed by a connection pool
   */
  def createAsync(name: String, dbConfig: DatabaseConfig, configuration: Config): AsyncDataSource

  def asJava: play.db.AsyncConnectionPool = new play.db.AsyncConnectionPool {
    override def createAsync(name: String, configuration: Config, environment: play.Environment): play.db.AsyncDataSource = {
      top.createAsync(name, DatabaseConfig.fromConfig(Configuration(configuration), environment.asScala), configuration).asJava
    }
    override def asScala(): AsyncConnectionPool = top
  }

  @deprecated("This method only exists to provide compatibility while ConnectionPool remains in the Play API", "2.6.0")
  private[play] def toConnectionPool: ConnectionPool = new ConnectionPool {
    override def create(name: String, dbConfig: DatabaseConfig, configuration: Config): DataSource = top.createAsync(name, dbConfig, configuration)
    override def close(dataSource: DataSource): Unit = dataSource match {
      case adb: AsyncDataSource => adb.close()
      case _ => throw new IllegalArgumentException(s"ConnectionPool.close must be passed the DataSource object that it created: $dataSource")
    }
  }

}

object AsyncConnectionPool {

  /**
   * Load a connection pool from a configured connection pool
   */
  def fromConfig(config: String, injector: Injector, environment: Environment, default: AsyncConnectionPool): AsyncConnectionPool = {
    config match {
      case "default" => default
      case "bonecp" => new BoneConnectionPool(environment)
      case "hikaricp" => new HikariCPConnectionPool(environment)
      case fqcn =>
        val cls = Reflect.getClass[ConnectionPool](fqcn, environment.classLoader)
        val cp = injector.instanceOf(cls)
        cp match {
          case acp: AsyncConnectionPool => acp
          case cp: ConnectionPool => fromConnectionPool(cp)
        }
    }
  }

  def fromConnectionPool(cp: ConnectionPool): AsyncConnectionPool = new AsyncConnectionPool {
    override def createAsync(name: String, dbConfig: DatabaseConfig, configuration: Config): AsyncDataSource = {
      val ds = cp.create(name, dbConfig, configuration)
      AsyncDataSource.wrap(ds, () => cp.close(ds))
    }
  }

}

