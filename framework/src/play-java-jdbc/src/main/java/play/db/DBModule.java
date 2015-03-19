/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;

import scala.collection.Seq;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import play.db.NamedDatabase;
import play.db.NamedDatabaseImpl;
import play.libs.Scala;

import com.google.common.collect.ImmutableList;

/**
 * Injection module with default DB components.
 */
public class DBModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        String dbKey = configuration.underlying().getString("play.db.config");
        String defaultDb = configuration.underlying().getString("play.db.default");

        ImmutableList.Builder<Binding<?>> list = new ImmutableList.Builder<Binding<?>>();

        list.add(bind(ConnectionPool.class).to(DefaultConnectionPool.class));
        list.add(bind(DBApi.class).to(DefaultDBApi.class));

        try {
            Set<String> dbs = configuration.underlying().getConfig(dbKey).root().keySet();
            for (String db : dbs) {
                list.add(bind(Database.class).qualifiedWith(named(db)).to(new NamedDatabaseProvider(db)));
            }

            if (dbs.contains(defaultDb)) {
                list.add(bind(Database.class).to(bind(Database.class).qualifiedWith(named(defaultDb))));
            }
        } catch (com.typesafe.config.ConfigException.Missing e) {
            // ignore missing configuration
        }

        return Scala.toSeq(list.build());
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
