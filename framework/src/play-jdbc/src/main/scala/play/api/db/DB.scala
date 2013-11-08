package play.api.db

import scala.language.reflectiveCalls

import play.api._
import play.api.libs._

import play.core._

import java.sql._
import javax.sql._

import com.jolbox.bonecp._
import com.jolbox.bonecp.hooks._
import scala.util.control.{ NonFatal, ControlThrowable }

/**
 * The Play Database API manages several connection pools.
 *
 * @param datasources the managed data sources
 */
trait DBApi {

  val datasources: List[(DataSource, String)]

  /**
   * Shutdown pool for given datasource
   */
  def shutdownPool(ds: DataSource)

  /**
   * Retrieves a JDBC connection, with auto-commit set to `true`.
   *
   * Don't forget to release the connection at some point by calling close().
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
   * @return The JDBC URL connection string, i.e. `jdbc:...`
   * @throws an error if the required data source is not registered
   */
  def getDataSourceURL(name: String): String = {
    val connection = getDataSource(name).getConnection
    val url = connection.getMetaData.getURL
    connection.close()
    url
  }

  /**
   * Retrieves a JDBC connection.
   *
   * Don't forget to release the connection at some point by calling close().
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
   * Execute a block of code, providing a JDBC connection. The connection and all created statements are
   * automatically released.
   *
   * @param name The datasource name.
   * @param block Code block to execute.
   */
  def withConnection[A](name: String)(block: Connection => A): A = {
    val connection = new AutoCleanConnection(getConnection(name))
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection and all created statements are automatically released.
   * The transaction is automatically committed, unless an exception occurs.
   *
   * @param name The datasource name.
   * @param block Code block to execute.
   */
  def withTransaction[A](name: String)(block: Connection => A): A = {
    withConnection(name) { connection =>
      try {
        connection.setAutoCommit(false)
        val r = block(connection)
        connection.commit()
        r
      } catch {
        case e: ControlThrowable => connection.commit(); throw e
        case NonFatal(e) => connection.rollback(); throw e
      }
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
   * Execute a block of code, providing a JDBC connection. The connection and all created statements are
   * automatically released.
   *
   * @param block Code block to execute.
   */
  def withConnection[A](block: Connection => A)(implicit app: Application): A = {
    app.plugin[DBPlugin].map(_.api.withConnection("default")(block)).getOrElse(error)
  }

  /**
   * Execute a block of code, in the scope of a JDBC transaction.
   * The connection and all created statements are automatically released.
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
   * The connection and all created statements are automatically released.
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

  private def dbURL(conn: Connection): String = {
    val u = conn.getMetaData.getURL
    conn.close()
    u
  }

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
    // Try to connect to each, this should be the first access to dbApi
    dbApi.datasources.map { ds =>
      try {
        ds._1.getConnection.close()
        app.mode match {
          case Mode.Test =>
          case mode => Play.logger.info("database [" + ds._2 + "] connected at " + dbURL(ds._1.getConnection))
        }
      } catch {
        case NonFatal(e) => {
          throw dbConfig.reportError(ds._2 + ".url", "Cannot connect to database [" + ds._2 + "]", Some(e.getCause))
        }
      }
    }
  }

  /**
   * Closes all data sources.
   */
  override def onStop() {
    dbApi.datasources.foreach {
      case (ds, _) => try {
        dbApi.shutdownPool(ds)
      } catch { case NonFatal(_) => }
    }
    val drivers = DriverManager.getDrivers()
    while (drivers.hasMoreElements) {
      val driver = drivers.nextElement
      DriverManager.deregisterDriver(driver)
    }
  }

}

private[db] class BoneCPApi(configuration: Configuration, classloader: ClassLoader) extends DBApi {

  private def error(db: String, message: String = "") = throw configuration.reportError(db, message)

  private val dbNames = configuration.subKeys

  private def register(driver: String, c: Configuration) {
    try {
      DriverManager.registerDriver(new play.utils.ProxyDriver(Class.forName(driver, true, classloader).newInstance.asInstanceOf[Driver]))
    } catch {
      case NonFatal(e) => throw c.reportError("driver", "Driver not found: [" + driver + "]", Some(e))
    }
  }

  private def createDataSource(dbName: String, url: String, driver: String, conf: Configuration): DataSource = {

    val datasource = new BoneCPDataSource

    // Try to load the driver
    conf.getString("driver").map { driver =>
      try {
        DriverManager.registerDriver(new play.utils.ProxyDriver(Class.forName(driver, true, classloader).newInstance.asInstanceOf[Driver]))
      } catch {
        case NonFatal(e) => throw conf.reportError("driver", "Driver not found: [" + driver + "]", Some(e))
      }
    }

    val autocommit = conf.getBoolean("autocommit").getOrElse(true)
    val isolation = conf.getString("isolation").map {
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
          logger.trace("Check in connection %s [%s leased]".format(connection.toString, datasource.getTotalLeased))
        }
      }

      override def onCheckOut(connection: ConnectionHandle) {
        connection.setAutoCommit(autocommit)
        isolation.map(connection.setTransactionIsolation(_))
        connection.setReadOnly(readOnly)
        catalog.map(connection.setCatalog(_))
        if (logger.isTraceEnabled) {
          logger.trace("Check out connection %s [%s leased]".format(connection.toString, datasource.getTotalLeased))
        }
      }

    })

    val PostgresFullUrl = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlFullUrl = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlCustomProperties = ".*\\?(.*)".r
    val H2DefaultUrl = "^jdbc:h2:mem:.+".r

    conf.getString("url") match {
      case Some(PostgresFullUrl(username, password, host, dbname)) =>
        datasource.setJdbcUrl("jdbc:postgresql://%s/%s".format(host, dbname))
        datasource.setUsername(username)
        datasource.setPassword(password)
      case Some(url @ MysqlFullUrl(username, password, host, dbname)) =>
        val defaultProperties = """?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"""
        val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
        datasource.setJdbcUrl("jdbc:mysql://%s/%s".format(host, dbname + addDefaultPropertiesIfNeeded))
        datasource.setUsername(username)
        datasource.setPassword(password)
      case Some(url @ H2DefaultUrl()) if !url.contains("DB_CLOSE_DELAY") =>
        if (Play.maybeApplication.exists(_.mode == Mode.Dev)) {
          datasource.setJdbcUrl(url + ";DB_CLOSE_DELAY=-1")
        } else {
          datasource.setJdbcUrl(url)
        }
      case Some(s: String) =>
        datasource.setJdbcUrl(s)
      case _ =>
        throw conf.globalError("Missing url configuration for database [%s]".format(conf))
    }

    conf.getString("user").map(datasource.setUsername(_))
    conf.getString("pass").map(datasource.setPassword(_))
    conf.getString("password").map(datasource.setPassword(_))

    // Pool configuration
    datasource.setPartitionCount(conf.getInt("partitionCount").getOrElse(1))
    datasource.setMaxConnectionsPerPartition(conf.getInt("maxConnectionsPerPartition").getOrElse(30))
    datasource.setMinConnectionsPerPartition(conf.getInt("minConnectionsPerPartition").getOrElse(5))
    datasource.setAcquireIncrement(conf.getInt("acquireIncrement").getOrElse(1))
    datasource.setAcquireRetryAttempts(conf.getInt("acquireRetryAttempts").getOrElse(10))
    datasource.setAcquireRetryDelayInMs(conf.getMilliseconds("acquireRetryDelay").getOrElse(1000))
    datasource.setConnectionTimeoutInMs(conf.getMilliseconds("connectionTimeout").getOrElse(1000))
    datasource.setIdleMaxAge(conf.getMilliseconds("idleMaxAge").getOrElse(1000 * 60 * 10), java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setMaxConnectionAge(conf.getMilliseconds("maxConnectionAge").getOrElse(1000 * 60 * 60), java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setDisableJMX(conf.getBoolean("disableJMX").getOrElse(true))
    datasource.setStatisticsEnabled(conf.getBoolean("statisticsEnabled").getOrElse(false))
    datasource.setIdleConnectionTestPeriod(conf.getMilliseconds("idleConnectionTestPeriod").getOrElse(1000 * 60), java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setDisableConnectionTracking(conf.getBoolean("disableConnectionTracking").getOrElse(true))

    conf.getString("initSQL").map(datasource.setInitSQL(_))
    conf.getBoolean("logStatements").map(datasource.setLogStatementsEnabled(_))
    conf.getString("connectionTestStatement").map(datasource.setConnectionTestStatement(_))

    // Bind in JNDI
    conf.getString("jndiName").map { name =>
      JNDI.initialContext.rebind(name, datasource)
      Play.logger.info("datasource [" + conf.getString("url").get + "] bound to JNDI as " + name)
    }

    datasource

  }

  val datasources: List[Tuple2[DataSource, String]] = dbNames.map { dbName =>
    val url = configuration.getString(dbName + ".url").getOrElse(error(dbName, "Missing configuration [db." + dbName + ".url]"))
    val driver = configuration.getString(dbName + ".driver").getOrElse(error(dbName, "Missing configuration [db." + dbName + ".driver]"))
    val extraConfig = configuration.getConfig(dbName).getOrElse(error(dbName, "Missing configuration [db." + dbName + "]"))
    register(driver, extraConfig)
    createDataSource(dbName, url, driver, extraConfig) -> dbName
  }.toList

  def shutdownPool(ds: DataSource) = {
    ds match {
      case ds: BoneCPDataSource => ds.close()
      case _ => error(" - could not recognize DataSource, therefore unable to shutdown this pool")
    }
  }

  /**
   * Retrieves a JDBC connection, with auto-commit set to `true`.
   *
   * Don't forget to release the connection at some point by calling close().
   *
   * @param name the data source name
   * @return a JDBC connection
   * @throws an error if the required data source is not registered
   */
  def getDataSource(name: String): DataSource = {
    datasources.filter(_._2 == name).headOption.map(e => e._1).getOrElse(error(" - could not find datasource for " + name))
  }

}

/**
 * Provides an interface for retreiving the jdbc driver's implementation of java.sql.Connection
 * from a "decorated" Connection (such as the Connection that DB.withConnection provides). Upcasting
 * to this trait should be used with caution since exposing the internal jdbc connection can violate the
 * guarantees Play otherwise makes (like automatically closing jdbc statements created from the connection)
 */
trait HasInternalConnection {
  def getInternalConnection(): Connection
}

/**
 * A connection that automatically releases statements on close
 */
private class AutoCleanConnection(connection: Connection) extends Connection with HasInternalConnection {

  private val statements = scala.collection.mutable.ListBuffer.empty[Statement]

  private def registering[T <: Statement](b: => T) = {
    val statement = b
    statements += statement
    statement
  }

  private def releaseStatements() {
    statements.foreach { statement =>
      statement.close()
    }
    statements.clear()
  }

  override def getInternalConnection(): Connection = connection match {
    case bonecpConn: com.jolbox.bonecp.ConnectionHandle =>
      bonecpConn.getInternalConnection()
    case x => x
  }

  def createStatement() = registering(connection.createStatement())
  def createStatement(resultSetType: Int, resultSetConcurrency: Int) = registering(connection.createStatement(resultSetType, resultSetConcurrency))
  def createStatement(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability))
  def prepareStatement(sql: String) = registering(connection.prepareStatement(sql))
  def prepareStatement(sql: String, autoGeneratedKeys: Int) = registering(connection.prepareStatement(sql, autoGeneratedKeys))
  def prepareStatement(sql: String, columnIndexes: scala.Array[Int]) = registering(connection.prepareStatement(sql, columnIndexes))
  def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int) = registering(connection.prepareStatement(sql, resultSetType, resultSetConcurrency))
  def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability))
  def prepareStatement(sql: String, columnNames: scala.Array[String]) = registering(connection.prepareStatement(sql, columnNames))
  def prepareCall(sql: String) = registering(connection.prepareCall(sql))
  def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int) = registering(connection.prepareCall(sql, resultSetType, resultSetConcurrency))
  def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability))

  def close() {
    releaseStatements()
    connection.close()
  }

  def clearWarnings() { connection.clearWarnings() }
  def commit() { connection.commit() }
  def createArrayOf(typeName: String, elements: scala.Array[AnyRef]) = connection.createArrayOf(typeName, elements)
  def createBlob() = connection.createBlob()
  def createClob() = connection.createClob()
  def createNClob() = connection.createNClob()
  def createSQLXML() = connection.createSQLXML()
  def createStruct(typeName: String, attributes: scala.Array[AnyRef]) = connection.createStruct(typeName, attributes)
  def getAutoCommit() = connection.getAutoCommit()
  def getCatalog() = connection.getCatalog()
  def getClientInfo() = connection.getClientInfo()
  def getClientInfo(name: String) = connection.getClientInfo(name)
  def getHoldability() = connection.getHoldability()
  def getMetaData() = connection.getMetaData()
  def getTransactionIsolation() = connection.getTransactionIsolation()
  def getTypeMap() = connection.getTypeMap()
  def getWarnings() = connection.getWarnings()
  def isClosed() = connection.isClosed()
  def isReadOnly() = connection.isReadOnly()
  def isValid(timeout: Int) = connection.isValid(timeout)
  def nativeSQL(sql: String) = connection.nativeSQL(sql)
  def releaseSavepoint(savepoint: Savepoint) { connection.releaseSavepoint(savepoint) }
  def rollback() { connection.rollback() }
  def rollback(savepoint: Savepoint) { connection.rollback(savepoint) }
  def setAutoCommit(autoCommit: Boolean) { connection.setAutoCommit(autoCommit) }
  def setCatalog(catalog: String) { connection.setCatalog(catalog) }
  def setClientInfo(properties: java.util.Properties) { connection.setClientInfo(properties) }
  def setClientInfo(name: String, value: String) { connection.setClientInfo(name, value) }
  def setHoldability(holdability: Int) { connection.setHoldability(holdability) }
  def setReadOnly(readOnly: Boolean) { connection.setReadOnly(readOnly) }
  def setSavepoint() = connection.setSavepoint()
  def setSavepoint(name: String) = connection.setSavepoint(name)
  def setTransactionIsolation(level: Int) { connection.setTransactionIsolation(level) }
  def setTypeMap(map: java.util.Map[String, Class[_]]) { connection.setTypeMap(map) }
  def isWrapperFor(iface: Class[_]) = connection.isWrapperFor(iface)
  def unwrap[T](iface: Class[T]) = connection.unwrap(iface)

  // JDBC 4.1
  def getSchema() = {
    connection.asInstanceOf[{ def getSchema(): String }].getSchema()
  }

  def setSchema(schema: String) {
    connection.asInstanceOf[{ def setSchema(schema: String): Unit }].setSchema(schema)
  }

  def getNetworkTimeout() = {
    connection.asInstanceOf[{ def getNetworkTimeout(): Int }].getNetworkTimeout()
  }

  def setNetworkTimeout(executor: java.util.concurrent.Executor, milliseconds: Int) {
    connection.asInstanceOf[{ def setNetworkTimeout(executor: java.util.concurrent.Executor, milliseconds: Int): Unit }].setNetworkTimeout(executor, milliseconds)
  }

  def abort(executor: java.util.concurrent.Executor) {
    connection.asInstanceOf[{ def abort(executor: java.util.concurrent.Executor): Unit }].abort(executor)
  }

}
