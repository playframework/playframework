/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

/** Injection module with default DB components. */
public final class DBModule extends Module {

  private static final Logger logger = LoggerFactory.getLogger(DBModule.class);

  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    String dbKey = config.getString("play.db.config");
    String defaultDb = config.getString("play.db.default");

    ImmutableList.Builder<Binding<?>> list = new ImmutableList.Builder<>();

    list.add(bindClass(ConnectionPool.class).to(DefaultConnectionPool.class));
    list.add(bindClass(DBApi.class).to(DefaultDBApi.class));

    try {
      Set<String> dbs = config.getConfig(dbKey).root().keySet();
      for (String db : dbs) {
        list.add(
            bindClass(Database.class).qualifiedWith(named(db)).to(new NamedDatabaseProvider(db)));
      }

      if (dbs.contains(defaultDb)) {
        list.add(
            bindClass(Database.class)
                .to(bindClass(Database.class).qualifiedWith(named(defaultDb))));
      }
    } catch (com.typesafe.config.ConfigException.Missing ex) {
      logger.warn("Configuration not found for database: {}", ex.getMessage());
    }

    return list.build();
  }

  private NamedDatabase named(String name) {
    return new NamedDatabaseImpl(name);
  }

  /** Inject provider for named databases. */
  public static class NamedDatabaseProvider implements Provider<Database> {
    @Inject private DBApi dbApi = null;
    private final String name;

    public NamedDatabaseProvider(String name) {
      this.name = name;
    }

    public Database get() {
      return dbApi.getDatabase(name);
    }
  }
}
