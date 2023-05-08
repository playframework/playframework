/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import com.typesafe.config.Config;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.sql.DataSource;
import play.Environment;
import play.api.db.DatabaseConfig;

/** Default delegating implementation of the connection pool API. */
@Singleton
public class DefaultConnectionPool implements ConnectionPool {

  private final play.api.db.ConnectionPool cp;

  @Inject
  public DefaultConnectionPool(play.api.db.ConnectionPool connectionPool) {
    this.cp = connectionPool;
  }

  public DataSource create(String name, Config config, Environment environment) {
    return cp.create(
        name,
        DatabaseConfig.fromConfig(new play.api.Configuration(config), environment.asScala()),
        config);
  }

  public void close(DataSource dataSource) {
    cp.close(dataSource);
  }

  @Override
  public play.api.db.ConnectionPool asScala() {
    return cp;
  }
}
