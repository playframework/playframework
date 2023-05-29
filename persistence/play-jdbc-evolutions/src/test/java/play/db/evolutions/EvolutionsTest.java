/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.evolutions;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.*;
import play.Environment;
import play.Mode;
import play.api.Configuration;
import play.api.db.evolutions.DefaultEvolutionsConfigParser;
import play.api.db.evolutions.EvolutionsConfig;
import play.api.db.evolutions.EvolutionsReader;
import play.db.Database;
import play.db.Databases;

public class EvolutionsTest {
  private Database database;
  private Connection connection;

  @Test
  public void testEvolutions() throws Exception {
    final ClassLoader classLoader = this.getClass().getClassLoader();
    Environment environment = new Environment(new File("."), classLoader, Mode.TEST);
    Configuration configuration = Configuration.load(environment.asScala());
    EvolutionsConfig evolutionsConfig = new DefaultEvolutionsConfigParser(configuration).get();

    EvolutionsReader reader =
        Evolutions.fromClassLoader(evolutionsConfig, classLoader, "evolutionstest" + "/");
    Evolutions.applyEvolutions(database, reader);

    // Ensure evolutions were applied
    ResultSet resultSet = executeStatement("select * from test");
    assertTrue(resultSet.next());

    Evolutions.cleanupEvolutions(database);

    // Ensure tables don't exist
    Assert.assertThrows(SQLException.class, () -> executeStatement("select * from test"));
  }

  private ResultSet executeStatement(String statement) throws Exception {
    return connection.prepareStatement(statement).executeQuery();
  }

  @Before
  public void createDatabase() {
    database = Databases.inMemory();
    connection = database.getConnection();
  }

  @After
  public void shutdown() {
    database.shutdown();
    database = null;
  }
}
