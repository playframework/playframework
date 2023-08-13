/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

// #database-test
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.db.Database;
import play.db.Databases;
import play.db.evolutions.Evolution;
import play.db.evolutions.Evolutions;

class DatabaseTest {

  private Database database;

  @BeforeEach
  public void setupDatabase() {
    database = Databases.inMemory();
    Evolutions.applyEvolutions(
        database,
        Evolutions.forDefault(
            new Evolution(
                1,
                "create table test (id bigint not null, name varchar(255));",
                "drop table test;")));
  }

  @AfterEach
  public void shutdownDatabase() {
    Evolutions.cleanupEvolutions(database);
    database.shutdown();
  }

  @Test
  void testDatabase() throws Exception {
    Connection connection = database.getConnection();
    connection.prepareStatement("insert into test values (10, 'testing')").execute();

    assertTrue(
        connection.prepareStatement("select * from test where id = 10").executeQuery().next());
  }
}
// #database-test
