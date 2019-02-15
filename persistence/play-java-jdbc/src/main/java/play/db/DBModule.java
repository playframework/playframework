/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Set;

/**
 * Injection module with default DB components.
 */
public final class DBModule extends Module {

    private static final Logger logger = LoggerFactory.getLogger(DBModule.class);

    @Override
    public List<Binding<?>> bindings(final Environment environment, final Config config) {
        String dbKey = config.getString("play.db.config");
        String defaultDb = config.getString("play.db.default");

        ImmutableList.Builder<Binding<?>> list = new ImmutableList.Builder<Binding<?>>();

        list.add(bindClass(ConnectionPool.class).to(DefaultConnectionPool.class));
        list.add(bindClass(DBApi.class).to(DefaultDBApi.class));

        try {
            Set<String> dbs = config.getConfig(dbKey).root().keySet();
            for (String db : dbs) {
                list.add(bindClass(Database.class).qualifiedWith(named(db)).to(new NamedDatabaseProvider(db)));
            }

            if (dbs.contains(defaultDb)) {
                list.add(bindClass(Database.class).to(bindClass(Database.class).qualifiedWith(named(defaultDb))));
            }
        } catch (com.typesafe.config.ConfigException.Missing ex) {
            logger.warn("Configuration not found for database: {}", ex.getMessage());
        }

        return list.build();
    }

    private NamedDatabase named(String name) {
        return new NamedDatabaseImpl(name);
    }

    /**
     * Inject provider for named databases.
     */
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
