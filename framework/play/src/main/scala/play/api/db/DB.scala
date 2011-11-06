package play.api.db

import play.api._
import play.core._

import java.sql._
import javax.sql._

import com.jolbox.bonecp._
import com.jolbox.bonecp.hooks._

/**
 * The Play Database Api manages several connection pools.
 *
 * @param datasources The managed datasources.
 */
case class DBApi(datasources: Map[String, (BoneCPDataSource, String)]) {

  /**
   * Retrieve a JDBC connection. Don't forget to release the connection
   * at some point by calling close().
   *
   * @param name The datasource name.
   * @param autocommit Set this connection to autocommit.
   * @return An JDBC connection.
   * @throws An error is the required datasource is not registred.
   */
  def getConnection(name: String, autocommit: Boolean = true): Connection = {
    val connection = getDataSource(name).getConnection
    connection.setAutoCommit(autocommit)
    connection
  }

  /**
   * Retrieve a JDBC connection (autocommit is set to true).
   * Don't forget to release the connection at some point by calling close().
   *
   * @param name The datasource name.
   * @return An JDBC connection.
   * @throws An error is the required datasource is not registred.
   */
  def getDataSource(name: String): DataSource = {
    datasources.get(name).map { _._1 }.getOrElse {
      throw new Exception("No database [" + name + "] is registred")
    }
  }

  /**
   * Retrieve the JDBC connection URL for a particular datasource.
   *
   * @param name The datasource name.
   * @return The JDBC url connection String (ie. jdbc:...)
   * @throws An error is the required datasource is not registred.
   */
  def getDataSourceURL(name: String): String = {
    datasources.get(name).map { _._2 }.getOrElse {
      throw new Exception("No database [" + name + "] is registred")
    }
  }

  /**
   * Execute a block of code, providing a JDBC connection. The connection is
   * automatically released.
   *
   * @param name The datasource name.
   * @param block Code block to execute.
   */
  def withConnection[A](name: String)(block: Connection => A): A = {
    val connection = getConnection(name)
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection is automatically released.
   * The transaction is automatically committed, unless an exception occurs.
   *
   * @param name The datasource name.
   * @param block Code block to execute.
   */
  def withTransaction[A](name: String)(block: Connection => A): A = {
    val connection = getConnection(name)
    try {
      connection.setAutoCommit(false)
      val r = block(connection)
      connection.commit()
      r
    } catch {
      case e => connection.rollback(); throw e
    } finally {
      connection.close()
    }
  }

}

/**
 * This object contains helper to create datasource managed by the DBApi.
 */
object DBApi {

  /**
   * Create a new datasource from configuration.
   *
   * @param conf The configuration part related to this datasource.
   * @param classloader The classloader used to load the JDBC driver.
   */
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

    val logger = Logger("com.jolbox.bonecp")

    // Re-apply per connection config @ checkout
    datasource.setConnectionHook(new AbstractConnectionHook {

      override def onCheckIn(connection: ConnectionHandle) {
        if (logger.isTraceEnabled) {
          logger.trace("Check in connection [%s leased]".format(datasource.getTotalLeased))
        }
      }

      override def onCheckOut(connection: ConnectionHandle) {
        connection.setAutoCommit(autocommit)
        connection.setTransactionIsolation(isolation)
        connection.setReadOnly(readOnly)
        catalog.map(connection.setCatalog(_))
        if (logger.isTraceEnabled) {
          logger.trace("Check out connection [%s leased]".format(datasource.getTotalLeased))
        }
      }

    })

    // url is required
    conf.getString("url").map(datasource.setJdbcUrl(_)).orElse {
      throw conf.globalError("Missing url configuration for database [" + conf.root + "]")
    }

    conf.getString("user").map(datasource.setUsername(_))
    conf.getString("pass").map(datasource.setPassword(_))
    conf.getString("password").map(datasource.setPassword(_))

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

/**
 * This object provides high level API to get JDBC connections.
 *
 * Example:
 * {{{
 * val conn = DB.getConnection("customers")
 * }}}
 */
object DB {

  /**
   * the exception we are throwing
   */
  private def error = throw new Exception("seems like db plugin is not registered properly, so we can not make calls to it.")
  /**
   * Retrieve a JDBC connection.
   *
   * @param name Datasource name.
   * @param autocommit Set this connection to autocommit.
   * @return An JDBC connection.
   * @throws An error is the required datasource is not registred.
   */
  def getConnection(name: String = "default", autocommit: Boolean = true)(implicit app: Application): Connection = app.plugin[DBPlugin].map(_.api.getConnection(name, autocommit)).getOrElse(error)

  /**
   * Retrieve a JDBC connection (autocommit is set to true).
   *
   * @param name Datasource name.
   * @return An JDBC connection.
   * @throws An error is the required datasource is not registred.
   */
  def getDataSource(name: String = "default")(implicit app: Application): DataSource = app.plugin[DBPlugin].map(_.api.getDataSource(name)).getOrElse(error)

  /**
   * Execute a block of code, providing a JDBC connection. The connection is
   * automatically released.
   *
   * @param name The datasource name.
   * @param block Code block to execute.
   */
  def withConnection[A](name: String)(block: Connection => A)(implicit app: Application): A = {
    app.plugin[DBPlugin].map(_.api.withConnection(name)(block)).getOrElse(error)
  }

  /**
   * Execute a block of code, providing a JDBC connection. The connection is
   * automatically released.
   *
   * @param block Code block to execute.
   */
  def withConnection[A](block: Connection => A)(implicit app: Application): A = {
    app.plugin[DBPlugin].map(_.api.withConnection("default")(block)).getOrElse(error)
  }

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection is automatically released.
   * The transaction is automatically committed, unless an exception occurs.
   *
   * @param name The datasource name.
   * @param block Code block to execute.
   */
  def withTransaction[A](name: String = "default")(block: Connection => A)(implicit app: Application): A = {
    app.plugin[DBPlugin].map(_.api.withTransaction(name)(block)).getOrElse(error)
  }

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection is automatically released.
   * The transaction is automatically committed, unless an exception occurs.
   *
   * @param block Code block to execute.
   */
  def withTransaction[A](block: Connection => A)(implicit app: Application): A = {
    app.plugin[DBPlugin].map(_.api.withTransaction("default")(block)).getOrElse(error)
  }

}

/**
 * Play Plugin to manage datasources.
 *
 * @param app The application in which registering the plugin.
 */
class DBPlugin(app: Application) extends Plugin {

  private lazy val db = {
    DBApi(app.configuration.getSub("db").map { dbConf =>
      dbConf.subKeys.map { db =>
        db -> DBApi.createDataSource(dbConf.getSub(db).get, app.classloader)
      }.toMap
    }.getOrElse(Map.empty))
  }

  override def enabled = if (db.datasources.size > 0) true else false

  /**
   * Retrieve the underlying DB Api managing the datasources.
   */

  def api = db

  /**
   * Read the configuration and connect to every datasources.
   */
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

  /**
   * Close all datasources.
   */
  override def onStop {
    db.datasources.values.foreach {
      case (ds, _) => try { ds.close() } catch { case _ => }
    }
  }

}
