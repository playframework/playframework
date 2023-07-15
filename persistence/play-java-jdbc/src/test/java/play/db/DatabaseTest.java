/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.jdbcdslog.ConnectionPoolDataSourceProxy;
import org.junit.jupiter.api.Test;
import play.api.libs.JNDI;

class DatabaseTest {

  @Test
  void createDatabase() {
    Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test");
    assertEquals("test", db.getName());
    assertEquals("jdbc:h2:mem:test", db.getUrl());
    db.shutdown();
  }

  @Test
  void createDefaultDatabase() {
    Database db = Databases.createFrom("org.h2.Driver", "jdbc:h2:mem:default");
    assertEquals("default", db.getName());
    assertEquals("jdbc:h2:mem:default", db.getUrl());
    db.shutdown();
  }

  @Test
  void createConfiguredDatabase() throws Exception {
    Map<String, String> config = ImmutableMap.of("jndiName", "DefaultDS");
    Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test", config);
    assertEquals("test", db.getName());
    assertEquals("jdbc:h2:mem:test", db.getUrl());

    // Forces the data source initialization, and then JNDI registration.
    db.getDataSource();

    assertEquals(db.getDataSource(), JNDI.initialContext().lookup("DefaultDS"));
    db.shutdown();
  }

  @Test
  void createDefaultInMemoryDatabase() {
    Database db = Databases.inMemory();
    assertEquals("default", db.getName());
    assertEquals("jdbc:h2:mem:default", db.getUrl());
    db.shutdown();
  }

  @Test
  void createNamedInMemoryDatabase() {
    Database db = Databases.inMemory("test");
    assertEquals("test", db.getName());
    assertEquals("jdbc:h2:mem:test", db.getUrl());
    db.shutdown();
  }

  @Test
  void createInMemoryDatabaseWithUrlOptions() {
    Map<String, String> options = ImmutableMap.of("MODE", "MySQL");
    Map<String, Object> config = ImmutableMap.of();
    Database db = Databases.inMemory("test", options, config);

    assertEquals("test", db.getName());
    assertEquals("jdbc:h2:mem:test;MODE=MySQL", db.getUrl());

    db.shutdown();
  }

  @Test
  void createConfiguredInMemoryDatabase() throws Exception {
    Database db = Databases.inMemoryWith("jndiName", "DefaultDS");
    assertEquals("default", db.getName());
    assertEquals("jdbc:h2:mem:default", db.getUrl());

    // Forces the data source initialization, and then JNDI registration.
    db.getDataSource();

    assertEquals(db.getDataSource(), JNDI.initialContext().lookup("DefaultDS"));
    db.shutdown();
  }

  @Test
  void supplyConnections() throws Exception {
    Database db = Databases.inMemory("test-connection");

    try (Connection connection = db.getConnection()) {
      connection
          .createStatement()
          .execute("create table test (id bigint not null, name varchar(255))");
    }

    db.shutdown();
  }

  @Test
  void enableAutocommitByDefault() throws Exception {
    Database db = Databases.inMemory("test-autocommit");

    try (Connection c1 = db.getConnection();
        Connection c2 = db.getConnection()) {
      c1.createStatement().execute("create table test (id bigint not null, name varchar(255))");
      c1.createStatement().execute("insert into test (id, name) values (1, 'alice')");
      ResultSet results = c2.createStatement().executeQuery("select * from test");
      assertTrue(results.next());
      assertFalse(results.next());
    }

    db.shutdown();
  }

  @Test
  void provideConnectionHelpers() {
    Database db = Databases.inMemory("test-withConnection");

    db.withConnection(
        c -> {
          c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
          c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
        });

    boolean result =
        db.withConnection(
            c -> {
              ResultSet results = c.createStatement().executeQuery("select * from test");
              assertTrue(results.next());
              assertFalse(results.next());
              return true;
            });

    assertTrue(result);

    db.shutdown();
  }

  @Test
  void provideConnectionHelpersWithAutoCommitIsFalse() {
    Database db = Databases.inMemory("test-withConnection(autoCommit = false");

    db.withConnection(
        false,
        c -> {
          c.createStatement().execute("create table test (id bigint not null, name varchar(255))");
          c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
        });

    boolean result =
        db.withConnection(
            c -> {
              ResultSet results = c.createStatement().executeQuery("select * from test");
              assertFalse(results.next());
              return true;
            });

    assertTrue(result);

    db.shutdown();
  }

  @Test
  void provideTransactionHelper() {
    Database db = Databases.inMemory("test-withTransaction");

    boolean created =
        db.withTransaction(
            c -> {
              c.createStatement()
                  .execute("create table test (id bigint not null, name varchar(255))");
              c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
              return true;
            });

    assertTrue(created);

    db.withConnection(
        c -> {
          ResultSet results = c.createStatement().executeQuery("select * from test");
          assertTrue(results.next());
          assertFalse(results.next());
        });

    try {
      db.withTransaction(
          (Connection c) -> {
            c.createStatement().execute("insert into test (id, name) values (2, 'bob')");
            throw new RuntimeException("boom");
          });
    } catch (Exception e) {
      assertEquals("boom", e.getMessage());
    }

    db.withConnection(
        c -> {
          ResultSet results = c.createStatement().executeQuery("select * from test");
          assertTrue(results.next());
          assertFalse(results.next());
        });

    db.shutdown();
  }

  @Test
  void notSupplyConnectionsAfterShutdown() throws Exception {
    Database db = Databases.inMemory("test-shutdown");
    db.getConnection().close();
    db.shutdown();
    SQLException sqlException = assertThrows(SQLException.class, () -> db.getConnection().close());
    assertTrue(sqlException.getMessage().endsWith("has been closed."));
  }

  @Test
  void useConnectionPoolDataSourceProxyWhenLogSqlIsTrue() throws Exception {
    Map<String, String> config = ImmutableMap.of("jndiName", "DefaultDS", "logSql", "true");
    Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test", config);
    assertInstanceOf(ConnectionPoolDataSourceProxy.class, db.getDataSource());
    assertInstanceOf(
        ConnectionPoolDataSourceProxy.class, JNDI.initialContext().lookup("DefaultDS"));
    db.shutdown();
  }

  @Test
  void manualSetupTransactionIsolationLevel() throws Exception {
    Database db = Databases.inMemory("test-withTransaction");

    boolean created =
        db.withTransaction(
            TransactionIsolationLevel.Serializable,
            c -> {
              c.createStatement()
                  .execute("create table test (id bigint not null, name varchar(255))");
              c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
              return true;
            });

    assertTrue(created);

    db.withConnection(
        c -> {
          ResultSet results = c.createStatement().executeQuery("select * from test");
          assertTrue(results.next());
          assertFalse(results.next());
        });

    db.shutdown();
  }
}
