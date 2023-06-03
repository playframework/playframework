/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.evolutions;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.db.Database;
import play.db.Databases;

public class EvolutionsTest {
  private Database database;
  private Connection connection;

  @Test
  public void testEvolutions() throws Exception {
    Evolutions.applyEvolutions(
        database, Evolutions.fromClassLoader(this.getClass().getClassLoader(), "evolutionstest/"));

    // Ensure evolutions were applied
    ResultSet resultSet = executeStatement("select * from test");
    assertTrue(resultSet.next());

    Evolutions.cleanupEvolutions(database);

    assertThrows(
        SQLException.class,
        () -> executeStatement("select * from test"),
        "SQL statement should have thrown an exception");
  }

  private ResultSet executeStatement(String statement) throws Exception {
    return connection.prepareStatement(statement).executeQuery();
  }

  @BeforeEach
  public void createDatabase() {
    database = Databases.inMemory();
    connection = database.getConnection();
  }

  @AfterEach
  public void shutdown() {
    database.shutdown();
    database = null;
  }
}
