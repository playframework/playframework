/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package org.jdbcdslog

import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * This class is necessary because jdbcdslog proxies does not
 * exposes the target dataSource, which is necessary to shutdown
 * the pool.
 */
class LogSqlDataSource extends ConnectionPoolDataSourceProxy {

  override def getParentLogger: Logger = throw new SQLFeatureNotSupportedException

  def getTargetDatasource = this.targetDS.asInstanceOf[DataSource]

}
