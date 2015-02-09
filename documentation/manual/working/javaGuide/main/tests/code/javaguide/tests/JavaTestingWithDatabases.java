package javaguide.tests;

import com.google.common.collect.ImmutableMap;

import play.db.Database;

import play.db.evolutions.*;
import org.junit.*;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class JavaTestingWithDatabases {

    public static class NotTested {
        {
            //#database
            Database database = Database.createFrom(
                    "com.mysql.jdbc.Driver",
                    "jdbc:mysql://localhost/test"
            );
            //#database
        }

        {
            //#full-config
            Database database = Database.createFrom(
                    "mydatabase",
                    "com.mysql.jdbc.Driver",
                    "jdbc:mysql://localhost/test",
                    ImmutableMap.of(
                            "user", "test",
                            "password", "secret"
                    )
            );
            //#full-config

            //#shutdown
            database.shutdown();
            //#shutdown

        }

        public static class ExampleUnitTest {
            //#database-junit
            Database database;

            @Before
            public void createDatabase() {
                database = Database.createFrom(
                        "com.mysql.jdbc.Driver",
                        "jdbc:mysql://localhost/test"
                );
            }

            @After
            public void shutdownDatabase() {
                database.shutdown();
            }
            //#database-junit
        }

    }

    @Test
    public void inMemory() throws Exception {
        //#in-memory
        Database database = Database.inMemory();
        //#in-memory

        try {
            assertThat(database.getConnection().getMetaData().getDatabaseProductName(), equalTo("H2"));
        } finally {
            database.shutdown();
        }
    }

    @Test
    public void inMemoryFullConfig() throws Exception {
        //#in-memory-full-config
        Database database = Database.inMemory(
                "mydatabase",
                ImmutableMap.of(
                        "MODE", "MYSQL"
                ),
                ImmutableMap.of(
                        "logStatements", true
                )
        );
        //#in-memory-full-config

        try {
            assertThat(database.getConnection().getMetaData().getDatabaseProductName(), equalTo("H2"));
        } finally {
            //#in-memory-shutdown
            database.shutdown();
            //#in-memory-shutdown
        }
    }

    @Test
    public void evolutions() throws Exception {
        Database database = Database.inMemory();
        try {
            //#apply-evolutions
            Evolutions.applyEvolutions(database);
            //#apply-evolutions

            //#cleanup-evolutions
            Evolutions.cleanupEvolutions(database);
            //#cleanup-evolutions
        } finally {
            database.shutdown();
        }
    }

    @Test
    public void staticEvolutions() throws Exception {
        Database database = Database.inMemory();
        try {
            //#apply-evolutions-simple
            Evolutions.applyEvolutions(database, Evolutions.forDefault(
                new Evolution(
                    1,
                    "create table test (id bigint not null, name varchar(255));",
                    "drop table test;"
                )
            ));
            //#apply-evolutions-simple

            Connection connection = database.getConnection();
            connection.prepareStatement("insert into test values (10, 'testing')").execute();

            //#cleanup-evolutions-simple
            Evolutions.cleanupEvolutions(database);
            //#cleanup-evolutions-simple

            try {
                connection.prepareStatement("select * from test").executeQuery();
                fail();
            } catch (SQLException e) {
                // pass
            }
        } finally {
            database.shutdown();
        }
    }

    @Test
    public void customPathEvolutions() throws Exception {
        Database database = Database.inMemory();
        try {
            //#apply-evolutions-custom-path
            Evolutions.applyEvolutions(database,
                Evolutions.fromClassLoader(
                    getClass().getClassLoader(), "testdatabase/")
            );
            //#apply-evolutions-custom-path
        } finally {
            database.shutdown();
        }
    }
}