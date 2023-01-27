/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

// #database-test
import static org.junit.Assert.*;

import java.sql.Connection;
import org.junit.*;
import play.db.Database;
import play.db.Databases;
import play.db.evolutions.*;

public class DatabaseTest {

  Database database;

  @Before
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

  @After
  public void shutdownDatabase() {
    Evolutions.cleanupEvolutions(database);
    database.shutdown();
  }

  @Test
  public void testDatabase() throws Exception {
    Connection connection = database.getConnection();
    connection.prepareStatement("insert into test values (10, 'testing')").execute();

    assertTrue(
        connection.prepareStatement("select * from test where id = 10").executeQuery().next());
  }
}
// #database-test
