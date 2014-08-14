/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import play.libs.F.Tuple;
import play.libs.Scala;

/**
 * Default delegating implementation of the database API.
 */
public class DefaultDBApi implements DBApi {

    private final play.api.db.DBApi dbApi;

    @Inject
    public DefaultDBApi(play.api.db.DBApi dbApi) {
        this.dbApi = dbApi;
    }

    public List<Tuple<DataSource, String>> dataSources() {
        return Scala.asJavaTuples(dbApi.datasources());
    }

    public void shutdownPool(DataSource ds) {
        dbApi.shutdownPool(ds);
    }

    public DataSource getDataSource(String name) {
        return dbApi.getDataSource(name);
    }

    public String getDataSourceURL(String name) {
        return dbApi.getDataSourceURL(name);
    }

    public Connection getConnection(String name, boolean autocommit) {
        return dbApi.getConnection(name, autocommit);
    }

    public <A> A withConnection(String name, ConnectionCallable<A> block) {
        return dbApi.withConnection(name, DB.connectionFunction(block));
    }

    public <A> A withTransaction(String name, ConnectionCallable<A> block) {
        return dbApi.withTransaction(name, DB.connectionFunction(block));
    }

}
