/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.jdbcdslog.ConnectionPoolDataSourceProxy;
import org.junit.Test;
import play.api.libs.JNDI;

public class DatabaseTest {

  @Test
  public void createDatabase() {
    Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test");
    assertThat(db.getName()).isEqualTo("test");
    assertThat(db.getUrl()).isEqualTo("jdbc:h2:mem:test");
    db.shutdown();
  }

  @Test
  public void createDefaultDatabase() {
    Database db = Databases.createFrom("org.h2.Driver", "jdbc:h2:mem:default");
    assertThat(db.getName()).isEqualTo("default");
    assertThat(db.getUrl()).isEqualTo("jdbc:h2:mem:default");
    db.shutdown();
  }

  @Test
  public void createConfiguredDatabase() throws Exception {
    Map<String, String> config = ImmutableMap.of("jndiName", "DefaultDS");
    Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test", config);
    assertThat(db.getName()).isEqualTo("test");
    assertThat(db.getUrl()).isEqualTo("jdbc:h2:mem:test");

    // Forces the data source initialization, and then JNDI registration.
    db.getDataSource();

    assertThat(JNDI.initialContext().lookup("DefaultDS")).isEqualTo(db.getDataSource());
    db.shutdown();
  }

  @Test
  public void createDefaultInMemoryDatabase() {
    Database db = Databases.inMemory();
    assertThat(db.getName()).isEqualTo("default");
    assertThat(db.getUrl()).isEqualTo("jdbc:h2:mem:default");
    db.shutdown();
  }

  @Test
  public void createNamedInMemoryDatabase() {
    Database db = Databases.inMemory("test");
    assertThat(db.getName()).isEqualTo("test");
    assertThat(db.getUrl()).isEqualTo("jdbc:h2:mem:test");
    db.shutdown();
  }

  @Test
  public void createInMemoryDatabaseWithUrlOptions() {
    Map<String, String> options = ImmutableMap.of("MODE", "MySQL");
    Map<String, Object> config = ImmutableMap.of();
    Database db = Databases.inMemory("test", options, config);

    assertThat(db.getName()).isEqualTo("test");
    assertThat(db.getUrl()).isEqualTo("jdbc:h2:mem:test;MODE=MySQL");

    db.shutdown();
  }

  @Test
  public void createConfiguredInMemoryDatabase() throws Exception {
    Database db = Databases.inMemoryWith("jndiName", "DefaultDS");
    assertThat(db.getName()).isEqualTo("default");
    assertThat(db.getUrl()).isEqualTo("jdbc:h2:mem:default");

    // Forces the data source initialization, and then JNDI registration.
    db.getDataSource();

    assertThat(JNDI.initialContext().lookup("DefaultDS")).isEqualTo(db.getDataSource());
    db.shutdown();
  }

  @Test
  public void supplyConnections() throws Exception {
    Database db = Databases.inMemory("test-connection");

    try (Connection connection = db.getConnection()) {
      connection
          .createStatement()
          .execute("create table test (id bigint not null, name varchar(255))");
    }

    db.shutdown();
  }

  @Test
  public void enableAutocommitByDefault() throws Exception {
    Database db = Databases.inMemory("test-autocommit");

    try (Connection c1 = db.getConnection();
        Connection c2 = db.getConnection()) {
      c1.createStatement().execute("create table test (id bigint not null, name varchar(255))");
      c1.createStatement().execute("insert into test (id, name) values (1, 'alice')");
      ResultSet results = c2.createStatement().executeQuery("select * from test");
      assertThat(results.next()).isTrue();
      assertThat(results.next()).isFalse();
    }

    db.shutdown();
  }

  @Test
  public void provideConnectionHelpers() {
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
              assertThat(results.next()).isTrue();
              assertThat(results.next()).isFalse();
              return true;
            });

    assertThat(result).isTrue();

    db.shutdown();
  }

  @Test
  public void provideConnectionHelpersWithAutoCommitIsFalse() {
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
              assertThat(results.next()).isFalse();
              return true;
            });

    assertThat(result).isTrue();

    db.shutdown();
  }

  @Test
  public void provideTransactionHelper() {
    Database db = Databases.inMemory("test-withTransaction");

    boolean created =
        db.withTransaction(
            c -> {
              c.createStatement()
                  .execute("create table test (id bigint not null, name varchar(255))");
              c.createStatement().execute("insert into test (id, name) values (1, 'alice')");
              return true;
            });

    assertThat(created).isTrue();

    db.withConnection(
        c -> {
          ResultSet results = c.createStatement().executeQuery("select * from test");
          assertThat(results.next()).isTrue();
          assertThat(results.next()).isFalse();
        });

    try {
      db.withTransaction(
          (Connection c) -> {
            c.createStatement().execute("insert into test (id, name) values (2, 'bob')");
            throw new RuntimeException("boom");
          });
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("boom");
    }

    db.withConnection(
        c -> {
          ResultSet results = c.createStatement().executeQuery("select * from test");
          assertThat(results.next()).isTrue();
          assertThat(results.next()).isFalse();
        });

    db.shutdown();
  }

  @Test
  public void notSupplyConnectionsAfterShutdown() throws Exception {
    Database db = Databases.inMemory("test-shutdown");
    db.getConnection().close();
    db.shutdown();
    SQLException sqlException = assertThrows(SQLException.class, () -> db.getConnection().close());
    assertThat(sqlException.getMessage()).endsWith("has been closed.");
  }

  @Test
  public void useConnectionPoolDataSourceProxyWhenLogSqlIsTrue() throws Exception {
    Map<String, String> config = ImmutableMap.of("jndiName", "DefaultDS", "logSql", "true");
    Database db = Databases.createFrom("test", "org.h2.Driver", "jdbc:h2:mem:test", config);
    assertThat(db.getDataSource()).isInstanceOf(ConnectionPoolDataSourceProxy.class);
    assertThat(JNDI.initialContext().lookup("DefaultDS"))
        .isInstanceOf(ConnectionPoolDataSourceProxy.class);
    db.shutdown();
  }

  @Test
  public void manualSetupTransactionIsolationLevel() throws Exception {
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

    assertThat(created).isTrue();

    db.withConnection(
        c -> {
          ResultSet results = c.createStatement().executeQuery("select * from test");
          assertThat(results.next()).isTrue();
          assertThat(results.next()).isFalse();
        });

    db.shutdown();
  }
}
