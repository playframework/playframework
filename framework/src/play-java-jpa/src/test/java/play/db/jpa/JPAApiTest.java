/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import play.db.ConnectionRunnable;
import play.db.Database;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JPAApiTest {

    @Test
    public void insertAndFindEntities() throws Exception {
        TestDatabase db = new TestDatabase();

        db.jpa.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                TestEntity entity = new TestEntity();
                entity.id = 1L;
                entity.name = "alice";
                entity.save();
            }
        });

        db.jpa.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                TestEntity entity = TestEntity.find(1L);
                assertThat(entity.name, equalTo("alice"));
            }
        });

        db.shutdown();
    }

    public static class TestDatabase {
        final Database database;
        final JPAApi jpa;

        public TestDatabase() {
            database = Database.inMemoryWith("jndiName", "DefaultDS");
            execute("create table TestEntity (id bigint not null, name varchar(255));");
            jpa = JPA.createFor("defaultPersistenceUnit");
        }

        public void execute(final String sql) {
            database.withConnection(new ConnectionRunnable() {
                public void run(Connection connection) throws SQLException {
                    connection.createStatement().execute(sql);
                }
            });
        }

        public void shutdown() {
            jpa.shutdown();
            database.shutdown();
        }
    }

}
