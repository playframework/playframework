/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.tests;

//#database-test
import play.db.Database;
import play.db.evolutions.*;
import java.sql.Connection;

import org.junit.*;
import static org.junit.Assert.*;

public class DatabaseTest {

    Database database;

    @Before
    public void setupDatabase() {
        database = Database.inMemory();
        Evolutions.applyEvolutions(database, Evolutions.forDefault(new Evolution(
            1,
            "create table test (id bigint not null, name varchar(255));",
            "drop table test;"
        )));
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
            connection.prepareStatement("select * from test where id = 10")
                .executeQuery().next()
        );
    }
}
//#database-test

