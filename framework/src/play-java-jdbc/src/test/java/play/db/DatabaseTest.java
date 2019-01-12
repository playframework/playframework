/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.jdbcdslog.LogSqlDataSource;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

import play.api.libs.JNDI;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class DatabaseTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void createDatabase() {
        Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test");
        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test"));
        db.shutdown();
    }

    @Test
    public void createDefaultDatabase() {
        Database db = Databases.createFrom("org.h2.Driver", "jdbc:h2:mem:default");
        assertThat(db.getName(), equalTo("default"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:default"));
        db.shutdown();
    }

    @Test
    public void createConfiguredDatabase() throws Exception {
        Map<String, String> config = ImmutableMap.of("jndiName", "DefaultDS");
        Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test", config);
        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test"));

        // Forces the data source initialization, and then JNDI registration.
        db.getDataSource();

        assertThat(JNDI.initialContext().lookup("DefaultDS"), equalTo(db.getDataSource()));
        db.shutdown();
    }

    @Test
    public void createDefaultInMemoryDatabase() {
        Database db = Databases.inMemory();
        assertThat(db.getName(), equalTo("default"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:default"));
        db.shutdown();
    }

    @Test
    public void createNamedInMemoryDatabase() {
        Database db = Databases.inMemory("test");
        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test"));
        db.shutdown();
    }

    @Test
    public void createInMemoryDatabaseWithUrlOptions() {
        Map<String, String> options = ImmutableMap.of("MODE", "MySQL");
        Map<String, Object> config = ImmutableMap.<String, Object>of();
        Database db = Databases.inMemory("test", options, config);

        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test;MODE=MySQL"));

        db.shutdown();
    }

    @Test
    public void createConfiguredInMemoryDatabase() throws Exception {
        Database db = Databases.inMemoryWith("jndiName", "DefaultDS");
        assertThat(db.getName(), equalTo("default"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:default"));

        // Forces the data source initialization, and then JNDI registration.
        db.getDataSource();

        assertThat(JNDI.initialContext().lookup("DefaultDS"), equalTo(db.getDataSource()));
        db.shutdown();
    }

    @Test
    public void supplyConnections() throws Exception {
        Database db = Databases.inMemory("test-connection");

        try (Connection connection = db.getConnection()) {
            connection.createStatement().execute("create table test (id bigint not null, name varchar(255))");
        }

        db.shutdown();
    }

    @Test
    public void enableAutocommitByDefault() throws Exception {
        Database db = Databases.inMemory("test-autocommit");

        try (Connection c1 = db.getConnection(); Connection c2 = db.getConnection()) {
            c1.createStatement().execute("create table test (id bigint not null, name varchar(255))");
            c1.createStatement().execute("insert into test (id, name) values (1, 'alice')");
            ResultSet results = c2.createStatement().executeQuery("select * from test");
            assertThat(results.next(), is(true));
            assertThat(results.next(), is(false));
        }

        db.shutdown();
    }

    @Test
    public void provideConnectionHelpers() {
        Database db = Databases.inMemory("test-withConnection");

        db.withConnection(c -> {
            c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
            c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
        });

        boolean result = db.withConnection(c -> {
            ResultSet results = c.createStatement().executeQuery("select * from test");
            assertThat(results.next(), is(true));
            assertThat(results.next(), is(false));
            return true;
        });

        assertThat(result, is(true));

        db.shutdown();
    }

    @Test
    public void provideConnectionHelpersWithAutoCommitIsFalse() {
        Database db = Databases.inMemory("test-withConnection(autoCommit = false");

        db.withConnection(false, c -> {
            c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
            c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
        });

        boolean result = db.withConnection(c -> {
            ResultSet results = c.createStatement().executeQuery("select * from test");
            assertThat(results.next(), is(false));
            return true;
        });

        assertThat(result, is(true));

        db.shutdown();
    }

    @Test
    public void provideTransactionHelper() {
        Database db = Databases.inMemory("test-withTransaction");

        boolean created = db.withTransaction(c -> {
            c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
            c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
            return true;
        });

        assertThat(created, is(true));

        db.withConnection(c -> {
            ResultSet results = c.createStatement().executeQuery("select * from test");
            assertThat(results.next(), is(true));
            assertThat(results.next(), is(false));
        });

        try {
            db.withTransaction((Connection c) -> {
                c.createStatement().execute("insert into test (id, name) values (2, 'bob')");
                throw new RuntimeException("boom");
            });
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("boom"));
        }

        db.withConnection(c -> {
            ResultSet results = c.createStatement().executeQuery("select * from test");
            assertThat(results.next(), is(true));
            assertThat(results.next(), is(false));
        });

        db.shutdown();
    }

    @Test
    public void notSupplyConnectionsAfterShutdown() throws Exception {
        Database db = Databases.inMemory("test-shutdown");
        db.getConnection().close();
        db.shutdown();
        exception.expect(SQLException.class);
        exception.expectMessage(endsWith("has been closed."));
        db.getConnection().close();
    }

    @Test
    public void useLogSqlDataSourceWhenLogSqlIsTrue() throws Exception {
        Map<String, String> config = ImmutableMap.of("jndiName", "DefaultDS", "logSql", "true");
        Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test", config);
        assertThat(db.getDataSource(), instanceOf(LogSqlDataSource.class));
        assertThat(JNDI.initialContext().lookup("DefaultDS"), instanceOf(LogSqlDataSource.class));
        db.shutdown();
    }

    @Test
    public void manualSetupTrasactionIsolationLevel() throws Exception {
        Database db = Databases.inMemory("test-withTransaction");

        boolean created = db.withTransaction(TransactionIsolationLevel.Serializable, c -> {
            c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
            c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
            return true;
        });

        assertThat(created, is(true));

        db.withConnection(c -> {
            ResultSet results = c.createStatement().executeQuery("select * from test");
            assertThat(results.next(), is(true));
            assertThat(results.next(), is(false));
        });

        db.shutdown();
    }
}
