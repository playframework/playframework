/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import com.jolbox.bonecp.BoneCPDataSource;

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
    public void createDatabase() throws Exception {
        Database db = Database.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test");
        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test"));
        db.shutdown();
    }

    @Test
    public void createDefaultDatabase() throws Exception {
        Database db = Database.createFrom("org.h2.Driver", "jdbc:h2:mem:default");
        assertThat(db.getName(), equalTo("default"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:default"));
        db.shutdown();
    }

    @Test
    public void createConfiguredDatabase() throws Exception {
        Map<String, String> config = ImmutableMap.of("jndiName", "DefaultDS");
        Database db = Database.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test", config);
        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test"));
        assertThat((DataSource) JNDI.initialContext().lookup("DefaultDS"), equalTo(db.getDataSource()));
        db.shutdown();
    }

    @Test
    public void createDefaultInMemoryDatabase() throws Exception {
        Database db = Database.inMemory();
        assertThat(db.getName(), equalTo("default"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:default"));
        db.shutdown();
    }

    @Test
    public void createNamedInMemoryDatabase() throws Exception {
        Database db = Database.inMemory("test");
        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test"));
        db.shutdown();
    }

    @Test
    public void createInMemoryDatabaseWithUrlOptions() throws Exception {
        Map<String, String> options = ImmutableMap.of("MODE", "MySQL");
        Map<String, Object> config = ImmutableMap.<String, Object>of();
        Database db = Database.inMemory("test", options, config);

        assertThat(db.getName(), equalTo("test"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:test"));

        DataSource ds = db.getDataSource();
        if (ds instanceof BoneCPDataSource) {
            BoneCPDataSource bcp = (BoneCPDataSource) ds;
            assertThat(bcp.getJdbcUrl(), equalTo("jdbc:h2:mem:test;MODE=MySQL"));
        }

        db.shutdown();
    }

    @Test
    public void createConfiguredInMemoryDatabase() throws Exception {
        Database db = Database.inMemoryWith("jndiName", "DefaultDS");
        assertThat(db.getName(), equalTo("default"));
        assertThat(db.getUrl(), equalTo("jdbc:h2:mem:default"));
        assertThat((DataSource) JNDI.initialContext().lookup("DefaultDS"), equalTo(db.getDataSource()));
        db.shutdown();
    }

    @Test
    public void supplyConnections() throws Exception {
        Database db = Database.inMemory("test-connection");

        Connection connection = db.getConnection();

        try {
            connection.createStatement().execute("create table test (id bigint not null, name varchar(255))");
        } finally {
            connection.close();
        }

        db.shutdown();
    }

    @Test
    public void enableAutocommitByDefault() throws Exception {
        Database db = Database.inMemory("test-autocommit");

        Connection c1 = db.getConnection();
        Connection c2 = db.getConnection();

        try {
            c1.createStatement().execute("create table test (id bigint not null, name varchar(255))");
            c1.createStatement().execute("insert into test (id, name) values (1, 'alice')");
            ResultSet results = c2.createStatement().executeQuery("select * from test");
            assertThat(results.next(), is(true));
            assertThat(results.next(), is(false));
        } finally {
            c1.close();
            c2.close();
        }

        db.shutdown();
    }

    @Test
    public void provideConnectionHelpers() throws Exception {
        Database db = Database.inMemory("test-withConnection");

        db.withConnection(new ConnectionRunnable() {
            public void run(Connection c) throws SQLException {
                c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
                c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
            }
        });

        boolean result = db.withConnection(new ConnectionCallable<Boolean>() {
            public Boolean call(Connection c) throws SQLException {
                ResultSet results = c.createStatement().executeQuery("select * from test");
                assertThat(results.next(), is(true));
                assertThat(results.next(), is(false));
                return true;
            }
        });

        assertThat(result, is(true));

        db.shutdown();
    }

    @Test
    public void provideTransactionHelper() throws Exception {
        Database db = Database.inMemory("test-withTransaction");

        boolean created = db.withTransaction(new ConnectionCallable<Boolean>() {
            public Boolean call(Connection c) throws SQLException {
                c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
                c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
                return true;
            }
        });

        assertThat(created, is(true));

        db.withConnection(new ConnectionRunnable() {
            public void run(Connection c) throws SQLException {
                ResultSet results = c.createStatement().executeQuery("select * from test");
                assertThat(results.next(), is(true));
                assertThat(results.next(), is(false));
            }
        });

        try {
            db.withTransaction(new ConnectionRunnable() {
                public void run(Connection c) throws SQLException {
                    c.createStatement().execute("insert into test (id, name) values (2, 'bob')");
                    throw new RuntimeException("boom");
                }
            });
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo("boom"));
        }

        db.withConnection(new ConnectionRunnable() {
            public void run(Connection c) throws SQLException {
                ResultSet results = c.createStatement().executeQuery("select * from test");
                assertThat(results.next(), is(true));
                assertThat(results.next(), is(false));
            }
        });

        db.shutdown();
    }

    @Test
    public void notSupplyConnectionsAfterShutdown() throws Exception {
        Database db = Database.inMemory("test-shutdown");
        db.getConnection().close();
        db.shutdown();
        exception.expect(SQLException.class);
        exception.expectMessage(startsWith("Pool has been shutdown"));
        db.getConnection().close();
    }
}
