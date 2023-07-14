/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import play.db.Database;
import play.db.Databases;
import play.db.evolutions.Evolution;
import play.db.evolutions.Evolutions;

class JavaTestingWithDatabases {

  static class NotTested {
    {
      // #database
      Database database =
          Databases.createFrom("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/test");
      // #database
    }

    {
      // #full-config
      Database database =
          Databases.createFrom(
              "mydatabase",
              "com.mysql.jdbc.Driver",
              "jdbc:mysql://localhost/test",
              ImmutableMap.of(
                  "username", "test",
                  "password", "secret"));
      // #full-config

      // #shutdown
      database.shutdown();
      // #shutdown

    }

    static class ExampleUnitTest {
      // #database-junit
      static Database database;

      @BeforeAll
      static void createDatabase() {
        database = Databases.createFrom("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/test");
      }

      @AfterAll
      static void shutdownDatabase() {
        database.shutdown();
      }
      // #database-junit
    }
  }

  @Test
  void inMemory() throws Exception {
    // #in-memory
    Database database = Databases.inMemory();
    // #in-memory

    try {
      assertEquals("H2", database.getConnection().getMetaData().getDatabaseProductName());
    } finally {
      database.shutdown();
    }
  }

  @Test
  void inMemoryFullConfig() throws Exception {
    // #in-memory-full-config
    Database database =
        Databases.inMemory(
            "mydatabase", ImmutableMap.of("MODE", "MYSQL"), ImmutableMap.of("logStatements", true));
    // #in-memory-full-config

    try {
      assertEquals("H2", database.getConnection().getMetaData().getDatabaseProductName());
    } finally {
      // #in-memory-shutdown
      database.shutdown();
      // #in-memory-shutdown
    }
  }

  @Test
  void evolutions() throws Exception {
    Database database = Databases.inMemory();
    try {
      // #apply-evolutions
      Evolutions.applyEvolutions(database);
      // #apply-evolutions

      // #cleanup-evolutions
      Evolutions.cleanupEvolutions(database);
      // #cleanup-evolutions
    } finally {
      database.shutdown();
    }
  }

  @Test
  void staticEvolutions() throws Exception {
    Database database = Databases.inMemory();
    try {
      // #apply-evolutions-simple
      final Evolution evolution =
          new Evolution(
              1, "create table test (id bigint not null, name varchar(255));", "drop table test;");

      Evolutions.applyEvolutions(database, Evolutions.forDefault(evolution));
      // #apply-evolutions-simple

      Connection connection = database.getConnection();
      connection.prepareStatement("insert into test values (10, 'testing')").execute();

      // #cleanup-evolutions-simple
      Evolutions.cleanupEvolutions(database);
      // #cleanup-evolutions-simple

      assertThrows(
          SQLException.class,
          () -> connection.prepareStatement("select * from test").executeQuery());
    } finally {
      database.shutdown();
    }
  }

  @Test
  void customPathEvolutions() throws Exception {
    Database database = Databases.inMemory();
    try {
      // #apply-evolutions-custom-path
      Evolutions.applyEvolutions(
          database, Evolutions.fromClassLoader(getClass().getClassLoader(), "testdatabase/"));
      // #apply-evolutions-custom-path
    } finally {
      database.shutdown();
    }
  }
}
