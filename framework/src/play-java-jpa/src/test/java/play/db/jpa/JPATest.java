/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Test;
import play.Application;
import play.test.WithApplication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class JPATest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return fakeApplication(ImmutableMap.of(
            "db.default.driver", "org.h2.Driver",
            "db.default.url", "jdbc:h2:mem:play-test-jpa",
            "db.default.jndiName", "DefaultDS",
            "jpa.default", "defaultPersistenceUnit"
        ));
    }

    @Test
    public void insertAndFindEntities() {
        JPA.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                TestEntity entity = TestEntity.find(1L);
                assertThat(entity.name, equalTo("test1"));
            }
        });

        JPA.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                TestEntity entity = new TestEntity();
                entity.id = 2L;
                entity.name = "test2";
                entity.save();
            }
        });

        JPA.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                TestEntity entity = TestEntity.find(2L);
                assertThat(entity.name, equalTo("test2"));
            }
        });

        JPA.withTransaction(new play.libs.F.Callback0() {
            public void invoke() {
                List<String> names = TestEntity.allNames();
                assertThat(names.size(), equalTo(2));
                assertThat(names, hasItems("test1", "test2"));
            }
        });
    }
}
