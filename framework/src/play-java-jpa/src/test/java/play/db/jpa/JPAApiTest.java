/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import play.db.ConnectionCallable;
import play.db.DB;
import play.db.DBApi;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JPAApiTest {

    @Test
    public void insertAndFindEntities() throws Exception {
        DBApi db = DB.api(ImmutableMap.of(
            "default.driver", "org.h2.Driver",
            "default.url", "jdbc:h2:mem:play-test-jpa",
            "default.jndiName", "DefaultDS"
        ));

        db.withConnection("default", new ConnectionCallable<Boolean>() {
            public Boolean call(Connection connection) throws SQLException {
                return connection.createStatement().execute("create table TestEntity (id bigint not null, name varchar(255));");
            }
        });

        JPAConfig config = DefaultJPAConfig.from(ImmutableMap.of(
            "default", "defaultPersistenceUnit"
        ));

        JPAApi jpa = new DefaultJPAApi(config);

        jpa.start();

        jpa.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                TestEntity entity = new TestEntity();
                entity.id = 1L;
                entity.name = "alice";
                entity.save();
            }
        });

        jpa.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                TestEntity entity = TestEntity.find(1L);
                assertThat(entity.name, equalTo("alice"));
            }
        });

        jpa.shutdown();
        db.shutdown();
    }

}
