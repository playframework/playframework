/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.evolutions;

import org.junit.*;
import play.db.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class EvolutionsTest {
    private Database database;
    private Connection connection;

    @Test
    public void testEvolutions() throws Exception {
        Evolutions.applyEvolutions(database, Evolutions.fromClassLoader(this.getClass().getClassLoader(), "evolutionstest/"));

        // Ensure evolutions were applied
        ResultSet resultSet = executeStatement("select * from test");
        assertTrue(resultSet.next());

        Evolutions.cleanupEvolutions(database);
        try {
            // Ensure tables don't exist
            executeStatement("select * from test");
            fail("SQL statement should have thrown an exception");
        } catch (SQLException se) {
           // pass
        }
    }

    private ResultSet executeStatement(String statement) throws Exception {
        return connection.prepareStatement(statement).executeQuery();
    }

    @Before
    public void createDatabase() {
        database = Database.inMemory();
        connection = database.getConnection();
    }

    @After
    public void shutdown() {
        database.shutdown();
        database = null;
    }

}
