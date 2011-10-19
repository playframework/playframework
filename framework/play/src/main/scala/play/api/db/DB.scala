package play.api.db

import play.api._
import play.core._

import java.sql._
import javax.sql._

import com.jolbox.bonecp._
import com.jolbox.bonecp.hooks._

case class DBApi(datasources: Map[String, (BoneCPDataSource, String)]) {

  def getConnection(name: String, autocommit: Boolean = true): Connection = {
    val connection = getDataSource(name).getConnection
    connection.setAutoCommit(autocommit)
    connection
  }

  def getDataSource(name: String): DataSource = {
    datasources.get(name).map { _._1 }.getOrElse {
      throw new Exception("No database [" + name + "] is registred")
    }
  }

  def getDataSourceURL(name: String): String = {
    datasources.get(name).map { _._2 }.getOrElse {
      throw new Exception("No database [" + name + "] is registred")
    }
  }

}

object DBApi {

  def createDataSource(conf: Configuration, classloader: ClassLoader = ClassLoader.getSystemClassLoader) = {

    val datasource = new BoneCPDataSource

    // Try to load the driver
    conf.getString("driver").map { driver =>
      try {
        DriverManager.registerDriver(new play.utils.ProxyDriver(Class.forName(driver, true, classloader).newInstance.asInstanceOf[Driver]))
      } catch {
        case e => throw conf.reportError("driver", "Driver not found: [" + driver + "]", Some(e))
      }
    }

    val autocommit = conf.getBoolean("autocommit").getOrElse(true)
    val isolation = conf.getString("isolation").getOrElse("READ_COMMITTED") match {
      case "NONE" => Connection.TRANSACTION_NONE
      case "READ_COMMITTED" => Connection.TRANSACTION_READ_COMMITTED
      case "READ_UNCOMMITTED " => Connection.TRANSACTION_READ_UNCOMMITTED
      case "REPEATABLE_READ " => Connection.TRANSACTION_REPEATABLE_READ
      case "SERIALIZABLE" => Connection.TRANSACTION_SERIALIZABLE
      case unknown => throw conf.reportError("isolation", "Unknown isolation level [" + unknown + "]")
    }
    val catalog = conf.getString("defaultCatalog")
    val readOnly = conf.getBoolean("readOnly").getOrElse(false)

    datasource.setClassLoader(classloader)

    // Re-apply per connection config @ checkout
    datasource.setConnectionHook(new AbstractConnectionHook {
      override def onCheckOut(connection: ConnectionHandle) {
        connection.setAutoCommit(autocommit)
        connection.setTransactionIsolation(isolation)
        connection.setReadOnly(readOnly)
        catalog.map(connection.setCatalog(_))
      }
    })

    // url is required
    conf.getString("url").map(datasource.setJdbcUrl(_)).orElse {
      throw conf.globalError("Missing url configuration for database [" + conf.root + "]")
    }

    conf.getString("user").map(datasource.setUsername(_))
    conf.getString("pass").map(datasource.setPassword(_))

    // Pool configuration
    conf.getInt("partitionCount").map(datasource.setPartitionCount(_))
    conf.getInt("maxConnectionsPerPartition").map(datasource.setMaxConnectionsPerPartition(_))
    conf.getInt("minConnectionsPerPartition").map(datasource.setMinConnectionsPerPartition(_))
    conf.getInt("acquireIncrement").map(datasource.setAcquireIncrement(_))
    conf.getInt("acquireRetryAttempts").map(datasource.setAcquireRetryAttempts(_))
    conf.getInt("acquireRetryDelay").map(datasource.setAcquireRetryDelayInMs(_))
    conf.getInt("connectionTimeout").map(datasource.setConnectionTimeoutInMs(_))
    conf.getInt("idleMaxAge").map(datasource.setIdleMaxAgeInSeconds(_))
    conf.getString("initSQL").map(datasource.setInitSQL(_))
    conf.getBoolean("logStatements").map(datasource.setLogStatementsEnabled(_))
    conf.getInt("maxConnectionAge").map(datasource.setMaxConnectionAgeInSeconds(_))

    datasource -> conf.full("url")
  }

}

object DB {

  def getConnection(name: String = "default", autocommit: Boolean = true)(implicit app: Application): Connection = app.plugin[DBPlugin].api.getConnection(name, autocommit)
  def getDataSource(name: String = "default")(implicit app: Application): DataSource = app.plugin[DBPlugin].api.getDataSource(name)

}

class DBPlugin(app: Application) extends Plugin {

  lazy val db = {
    DBApi(app.configuration.getSub("db").map { dbConf =>
      dbConf.subKeys.map { db =>
        db -> DBApi.createDataSource(dbConf.getSub(db).get, app.classloader)
      }.toMap
    }.getOrElse(Map.empty))
  }

  def api = db

  override def onStart {
    db.datasources.map {
      case (name, (ds, config)) => {
        try {
          ds.getConnection.close()
          Logger("play").info("database [" + name + "] connected at " + ds.getJdbcUrl)
        } catch {
          case e => {
            throw app.configuration.reportError(config, "Cannot connect to database at [" + ds.getJdbcUrl + "]", Some(e.getCause))
          }
        }
      }
    }
  }

  override def onStop {
    db.datasources.values.foreach {
      case (ds, _) => try { ds.close() } catch { case _ => }
    }
  }

}