package play.api.db

import play.api._
import play.api.libs._

import play.core._

import java.sql._
import javax.sql._

import com.jolbox.bonecp._
import com.jolbox.bonecp.hooks._

/**
 * The Play Database API manages several connection pools.
 *
 * @param datasources the managed data sources
 */
trait DBApi {

  val datasources: List[Tuple2[DataSource, String]]
  
  /**
   * Shutdown pool for given datasource
   */
  def shutdownPool(ds: DataSource)
  
  /**
   * Retrieves a JDBC connection, with auto-commit set to `true`.
   *
   * Don’t forget to release the connection at some point by calling close().
   *
   * @param name the data source name
   * @return a JDBC connection
   * @throws an error if the required data source is not registered
   */
  def getDataSource(name: String): DataSource 

  /**
   * Retrieves the JDBC connection URL for a particular data source.
   *
   * @param name the data source name
   * @return The JDBC URL connection string, i.e. `jdbc:…`
   * @throws an error if the required data source is not registered
   */
  def getDataSourceURL(name: String): String = {
    val ds = getDataSource(name)
    ds.getConnection.getMetaData.getURL
  }
  
  /**
   * Retrieves a JDBC connection.
   *
   * Don’t forget to release the connection at some point by calling close().
   *
   * @param name the data source name
   * @param autocommit when `true`, sets this connection to auto-commit
   * @return a JDBC connection
   * @throws an error if the required data source is not registered
   */
  def getConnection(name: String, autocommit: Boolean = true): Connection = {
    val connection = getDataSource(name).getConnection
    connection.setAutoCommit(autocommit)
    connection
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
 * Provides a high-level API for getting JDBC connections.
 *
 * For example:
 * {{{
 * val conn = DB.getConnection("customers")
 * }}}
 */
object DB {

  /** The exception we are throwing. */
  private def error = throw new Exception("DB plugin is not registered.")

  /**
   * Retrieves a JDBC connection.
   *
   * @param name data source name
   * @param autocommit when `true`, sets this connection to auto-commit
   * @return a JDBC connection
   * @throws an error if the required data source is not registered
   */
  def getConnection(name: String = "default", autocommit: Boolean = true)(implicit app: Application): Connection = app.plugin[DBPlugin].map(_.api.getConnection(name, autocommit)).getOrElse(error)

  /**
   * Retrieves a JDBC connection (autocommit is set to true).
   *
   * @param name data source name
   * @return a JDBC connection
   * @throws an error if the required data source is not registered
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
 * Generic DBPlugin interface
 */
trait DBPlugin extends Plugin {
  def api: DBApi
}

/**
 * A DBPlugin implementation that provides a DBApi
 *
 * @param app the application that is registering the plugin
 */
class BoneCPPlugin(app: Application) extends DBPlugin {

  private def error = throw new Exception("db keys are missing from application.conf")

  lazy val dbConfig = app.configuration.getConfig("db").getOrElse(Configuration.empty)
  
  // should be accessed in onStart first
  private lazy val dbApi: DBApi = new BoneCPApi(dbConfig, app.classloader)
  
  /**
   * plugin is disabled if either configuration is missing or the plugin is explicitly disabled
   */
  private lazy val isDisabled = {
    app.configuration.getString("dbplugin").filter(_ == "disabled").isDefined || dbConfig.subKeys.isEmpty
  }
  
  /**
   * Is this plugin enabled.
   *
   * {{{
   * dbplugin=disabled
   * }}}
   */
  override def enabled = isDisabled == false

  /**
   * Retrieves the underlying `DBApi` managing the data sources.
   */
  def api: DBApi = dbApi

  /**
   * Reads the configuration and connects to every data source.
   */
  override def onStart() {
    //try to connect to each, this should be the first access to dbApi
    dbApi.datasources.map { ds =>
      try {
        ds._1.getConnection.close()
        app.mode match {
          case Mode.Test =>
          case mode => Logger("play").info("database [" + ds._2 + "] connected at " + ds._1.getConnection.getMetaData.getURL )
        }
      } catch {
        case e => {
          throw dbConfig.reportError("url", "Cannot connect to database at [" + ds._1.getConnection.getMetaData.getURL  + "]", Some(e.getCause))
        }
      }
    }
  }

  /**
   * Closes all data sources.
   */
  override def onStop() {
    dbApi.datasources.foreach {
      case (ds,_) => try {
        dbApi.shutdownPool(ds)
      } catch { case _ => }
    }
    val drivers = DriverManager.getDrivers()
    while (drivers.hasMoreElements) {
      val driver = drivers.nextElement
      DriverManager.deregisterDriver(driver)
    }
  }

}

private[db] class BoneCPApi(configuration: Configuration, classloader: ClassLoader) extends DBApi {

  private def error(m: String = "") = throw new Exception("Invalid DB configuration " + m)
  
  private val dbNames = configuration.subKeys

  private def register(driver: String, c: Configuration) {
    try {
      DriverManager.registerDriver(new play.utils.ProxyDriver(Class.forName(driver, true, classloader).newInstance.asInstanceOf[Driver]))
    } catch {
      case e => throw c.reportError("driver", "Driver not found: [" + driver + "]", Some(e))
    }
  }

  private def createDataSource(dbName: String, url: String, driver: String, conf: Configuration): DataSource = {
    
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
      throw conf.globalError("Missing url configuration for database [" + conf + "]")
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
    conf.getMilliseconds("acquireRetryDelay").map(datasource.setAcquireRetryDelayInMs(_))
    conf.getMilliseconds("connectionTimeout").map(datasource.setConnectionTimeoutInMs(_))
    conf.getMilliseconds("idleMaxAge").map(datasource.setIdleMaxAgeInSeconds(_))
    conf.getString("initSQL").map(datasource.setInitSQL(_))
    conf.getBoolean("logStatements").map(datasource.setLogStatementsEnabled(_))
    conf.getMilliseconds("maxConnectionAge").map(datasource.setMaxConnectionAge(_, java.util.concurrent.TimeUnit.MILLISECONDS))
    conf.getBoolean("disableJMX").orElse(Some(true)).map(datasource.setDisableJMX(_))
    conf.getString("connectionTestStatement").map(datasource.setConnectionTestStatement(_))

    // Bind in JNDI
    conf.getString("jndiName").map { name =>
      JNDI.initialContext.rebind(name, datasource)
      Logger("play").info("datasource [" + conf.getString("url").get + "] bound to JNDI as " + name)
    }
    
    datasource
                
  }
  
  //register either a specific connection or all of them 
  val datasources: List[Tuple2[DataSource, String]] = dbNames.map { dbName =>
      val url = configuration.getString(dbName + ".url").getOrElse(error("- could not determine url for " + dbName + ".url")) 
      val driver = configuration.getString(dbName + ".driver").getOrElse(error("- could not determine driver for " + dbName + ".driver"))    
      val extraConfig = configuration.getConfig(dbName).getOrElse(error("- could not find extra configs")) 
      register(driver, extraConfig)
      createDataSource(dbName, url, driver, extraConfig) -> dbName
    }.toList

  def shutdownPool(ds: DataSource) = ds match {
    case ds: BoneCPDataSource => ds.close()
    case _ => error(" - could not recognize DataSource, therefore unable to shutdown this pool")
  }

  /**
   * Retrieves a JDBC connection, with auto-commit set to `true`.
   *
   * Don’t forget to release the connection at some point by calling close().
   *
   * @param name the data source name
   * @return a JDBC connection
   * @throws an error if the required data source is not registered
   */
  def getDataSource(name: String): DataSource = {
    datasources.filter(_._2 == name).headOption.map(e => e._1).getOrElse(error(" - could not find datasource for "+name))
  }

}
