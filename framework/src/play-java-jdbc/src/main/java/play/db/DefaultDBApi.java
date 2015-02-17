/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import play.libs.Scala;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Default delegating implementation of the DB API.
 */
@Singleton
public class DefaultDBApi implements DBApi {

    private final play.api.db.DBApi dbApi;
    private final List<Database> databases;
    private final Map<String, Database> databaseByName;

    @Inject
    public DefaultDBApi(play.api.db.DBApi dbApi) {
        this.dbApi = dbApi;

        ImmutableList.Builder<Database> databases = new ImmutableList.Builder<Database>();
        ImmutableMap.Builder<String, Database> databaseByName = new ImmutableMap.Builder<String, Database>();
        for (play.api.db.Database db : Scala.asJava(dbApi.databases())) {
            Database database = new DefaultDatabase(db);
            databases.add(database);
            databaseByName.put(database.getName(), database);
        }
        this.databases = databases.build();
        this.databaseByName = databaseByName.build();
    }

    public List<Database> getDatabases() {
        return databases;
    }

    public Database getDatabase(String name) {
        return databaseByName.get(name);
    }

    public void shutdown() {
        dbApi.shutdown();
    }

}
