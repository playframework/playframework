/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package org.jdbcdslog;

import javax.sql.DataSource;

/**
 * This class is necessary because ConnectionPoolDataSourceProxy's targetDS field is protected. So
 * by defining this helper class, in Java and in the org.jdbcdslog, we can access the protected
 * field (because being in the package grants protected access).
 */
public class AccessConnectionPoolDataSourceProxy {
  public static DataSource getTargetDatasource(ConnectionPoolDataSourceProxy p) {
    return (DataSource) p.targetDS;
  }
}
