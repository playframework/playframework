package play.api.db.internal

import java.sql.Connection
import javax.sql.DataSource

import com.typesafe.config.Config
import org.jdbcdslog.{ ConnectionLogger, ConnectionLoggingProxy, LogSqlDataSource }

private[db] object SqlLogging {

  private[db] def isSqlLoggingConfigured(configuration: Config) = configuration.getBoolean("logSql")

  /**
   * Wraps a `DataSource` so that usage is logged. The caller should ensure
   * that this method is guarded by checking `isSqlLoggingConfigured`.
   */
  private[db] def wrapDataSourceForLogging(ds: DataSource): DataSource = {
    val proxyDataSource = new LogSqlDataSource()
    proxyDataSource.setTargetDSDirect(ds)
    proxyDataSource
  }

  /**
   * Wraps a `Connection` so that usage is logged. The caller should ensure
   * that this method is guarded by checking `isSqlLoggingConfigured`.
   */
  private[db] def wrapConnectionForLogging(c: Connection): Connection = {
    if (ConnectionLogger.isInfoEnabled) {
      ConnectionLogger.info("connect to URL " + c.getMetaData.getURL + " for user " + c.getMetaData.getUserName)
    }
    return ConnectionLoggingProxy.wrap(c)
  }

  /**
   * Unwraps a data source if it has been previously wrapped in a org.jdbcdslog.LogSqlDataSource.
   */
  private[db] def unwrap(dataSource: DataSource): DataSource = {
    dataSource match {
      case ds: LogSqlDataSource => ds.getTargetDatasource
      case _ => dataSource
    }
  }
}
