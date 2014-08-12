package play.api.db

import java.sql.{ Connection, Driver, DriverManager, Statement }
import javax.sql.DataSource

import scala.util.control.NonFatal

import play.api.{ Application, Configuration, Logger, Mode, Play }
import play.api.libs.JNDI

import com.jolbox.bonecp._
import com.jolbox.bonecp.hooks._

/**
 * BoneCP implementation of DBPlugin that provides a DBApi.
 *
 * @param app the application that is registering the plugin
 */
class BoneCPPlugin(app: Application) extends DBPlugin {
  lazy val dbConfig = app.configuration.getConfig("db").
    getOrElse(Configuration.empty)

  private def dbURL(conn: Connection): String = {
    val u = conn.getMetaData.getURL
    conn.close()
    u
  }

  // should be accessed in onStart first
  private lazy val dbApi = new BoneCPApi(dbConfig, app.classloader)

  /**
   * plugin is disabled if either configuration is missing or the plugin is explicitly disabled
   */
  private lazy val isDisabled = app.configuration.getString("dbplugin").
    exists(_ == "disabled") || dbConfig.subKeys.isEmpty

  /**
   * Is this plugin enabled.
   *
   * {{{
   * dbplugin=disabled
   * }}}
   */
  override def enabled = !isDisabled

  /**
   * Retrieves the underlying `DBApi` managing the data sources.
   */
  def api: DBApi = dbApi

  /**
   * Reads the configuration and connects to every data source.
   */
  override def onStart() {
    // Try to connect to each, this should be the first access to dbApi
    dbApi.datasources map { ds =>
      try {
        ds._1.getConnection.close()
        app.mode match {
          case Mode.Test =>
          case mode => Play.logger.info(s"database [${ds._2}] connected at ${dbURL(ds._1.getConnection)}")
        }
      } catch {
        case NonFatal(e) => {
          throw dbConfig.reportError(s"${ds._2}.url",
            s"Cannot connect to database [${ds._2}]", Some(e.getCause))
        }
      }
    }
  }

  /**
   * Closes all data sources.
   */
  override def onStop() {
    dbApi.datasources foreach {
      case (ds, _) => try {
        dbApi.shutdownPool(ds)
      } catch { case NonFatal(_) => }
    }
    dbApi.deregisterAll()
  }
}

private[db] class BoneCPApi(
    configuration: Configuration, classloader: ClassLoader) extends DBApi {

  private def error(db: String, message: String = "") =
    throw configuration.reportError(db, message)

  /**
   * @param d Driver class name
   * @param c DB configuration
   */
  private def register(d: String, c: Configuration): Driver = {
    try {
      val driver = new play.utils.ProxyDriver(
        Class.forName(d, true, classloader).newInstance.asInstanceOf[Driver])

      DriverManager.registerDriver(driver)
      driver
    } catch {
      case NonFatal(e) => throw c.reportError("driver",
        s"Driver not found: [$d]", Some(e))
    }
  }

  /** De-register all drivers this API has previously registered. */
  def deregisterAll(): Unit = drivers.foreach(DriverManager.deregisterDriver)

  private def createDataSource(dbName: String, conf: Configuration): (DataSource, Driver) = {

    val datasource = new BoneCPDataSource

    // Try to load the driver
    val d = configuration.getString(s"$dbName.driver").getOrElse(error(dbName, s"Missing configuration [db.$dbName.driver]"))

    val driver = register(d, conf)

    val autocommit = conf.getBoolean("autocommit").getOrElse(true)
    val isolation = conf.getString("isolation").map {
      case "NONE" => Connection.TRANSACTION_NONE
      case "READ_COMMITTED" => Connection.TRANSACTION_READ_COMMITTED
      case "READ_UNCOMMITTED" => Connection.TRANSACTION_READ_UNCOMMITTED
      case "REPEATABLE_READ" => Connection.TRANSACTION_REPEATABLE_READ
      case "SERIALIZABLE" => Connection.TRANSACTION_SERIALIZABLE
      case unknown => throw conf.reportError("isolation",
        s"Unknown isolation level [$unknown]")
    }
    val catalog = conf.getString("defaultCatalog")
    val readOnly = conf.getBoolean("readOnly").getOrElse(false)

    datasource.setClassLoader(classloader)

    val logger = Logger("com.jolbox.bonecp")

    // Re-apply per connection config @ checkout
    datasource.setConnectionHook(new AbstractConnectionHook {

      override def onCheckIn(connection: ConnectionHandle) {
        if (logger.isTraceEnabled) {
          logger.trace(s"Check in connection $connection [${datasource.getTotalLeased} leased]")
        }
      }

      override def onCheckOut(connection: ConnectionHandle) {
        connection.setAutoCommit(autocommit)
        isolation.map(connection.setTransactionIsolation)
        connection.setReadOnly(readOnly)
        catalog.map(connection.setCatalog)
        if (logger.isTraceEnabled) {
          logger.trace(s"Check out connection $connection [${datasource.getTotalLeased} leased]")
        }
      }

      override def onQueryExecuteTimeLimitExceeded(handle: ConnectionHandle, statement: Statement, sql: String, logParams: java.util.Map[AnyRef, AnyRef], timeElapsedInNs: Long) {
        val timeMs = timeElapsedInNs / 1000
        val query = PoolUtil.fillLogParams(sql, logParams)
        logger.warn(s"Query execute time limit exceeded (${timeMs}ms) - query: ${query}")
      }

    })

    val PostgresFullUrl = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlFullUrl = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlCustomProperties = ".*\\?(.*)".r
    val H2DefaultUrl = "^jdbc:h2:mem:.+".r

    conf.getString("url") match {
      case Some(PostgresFullUrl(username, password, host, dbname)) =>
        datasource.setJdbcUrl(s"jdbc:postgresql://$host/$dbname")
        datasource.setUsername(username)
        datasource.setPassword(password)

      case Some(url @ MysqlFullUrl(username, password, host, dbname)) =>
        val defaultProperties = "?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"
        val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
        datasource.setJdbcUrl(s"jdbc:mysql://$host/${dbname + addDefaultPropertiesIfNeeded}")
        datasource.setUsername(username)
        datasource.setPassword(password)

      case Some(url @ H2DefaultUrl()) if !url.contains("DB_CLOSE_DELAY") =>
        if (Play.maybeApplication.exists(_.mode == Mode.Dev)) {
          datasource.setJdbcUrl(s"$url;DB_CLOSE_DELAY=-1")
        } else datasource.setJdbcUrl(url)

      case Some(s: String) => datasource.setJdbcUrl(s)

      case _ => throw conf.globalError(s"Missing url configuration for database $dbName: $conf")
    }

    conf.getString("user").map(datasource.setUsername)
    conf.getString("pass").map(datasource.setPassword)
    conf.getString("password").map(datasource.setPassword)

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
    datasource.setQueryExecuteTimeLimitInMs(conf.getMilliseconds("queryExecuteTimeLimit").getOrElse(0))

    conf.getString("initSQL").map(datasource.setInitSQL)
    conf.getBoolean("logStatements").map(datasource.setLogStatementsEnabled)
    conf.getString("connectionTestStatement").map(datasource.setConnectionTestStatement)

    // Bind in JNDI
    conf.getString("jndiName") map { name =>
      JNDI.initialContext.rebind(name, datasource)
      Play.logger.info(s"""datasource [${conf.getString("url").get}] bound to JNDI as $name""")
    }

    datasource -> driver
  }

  @annotation.tailrec
  private def setupDatasources(dbNames: List[String], datasources: List[(DataSource, String)], dsMap: Map[String, DataSource], drivers: Set[Driver]): (List[(DataSource, String)], Map[String, DataSource], Set[Driver]) = dbNames match {
    case dbName :: ns =>
      val extraConfig = configuration.getConfig(dbName).getOrElse(error(dbName, s"Missing configuration [db.$dbName]"))
      val (ds, driver) = createDataSource(dbName, extraConfig)
      setupDatasources(ns, datasources :+ (ds -> dbName),
        dsMap + (dbName -> ds), drivers + driver)

    case _ => (datasources, dsMap, drivers)
  }

  private val (dsList, dsMap, drivers): (List[(DataSource, String)], Map[String, DataSource], Set[Driver]) = setupDatasources(configuration.subKeys.toList, Nil, Map.empty, Set.empty)

  val datasources = dsList

  def shutdownPool(ds: DataSource) = ds match {
    case bcp: BoneCPDataSource => bcp.close()
    case _ => error(" - could not recognize DataSource, therefore unable to shutdown this pool")
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
  def getDataSource(name: String): DataSource =
    dsMap.get(name).getOrElse(error(s" - could not find datasource for $name"))

}
