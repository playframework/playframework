/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.util.ArrayList;
import java.util.List;
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

/**
 * Injection module with default DB components.
 */
public class DBModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        if (configuration.underlying().getBoolean("play.modules.db.enabled")) {
            List<Binding<?>> list = new ArrayList<Binding<?>>();

            list.add(bind(ConnectionPool.class).to(DefaultConnectionPool.class));
            list.add(bind(DBApi.class).to(DefaultDBApi.class));
            list.add(bind(Database.class).to(bind(Database.class).qualifiedWith(named("default"))));

            Set<String> dbs = configuration.underlying().getConfig("db").root().keySet();
            for (String db : dbs) {
                list.add(bind(Database.class).qualifiedWith(named(db)).to(new NamedDatabaseProvider(db)));
            }

            return Scala.toSeq(list);
        } else {
            return seq();
        }
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
