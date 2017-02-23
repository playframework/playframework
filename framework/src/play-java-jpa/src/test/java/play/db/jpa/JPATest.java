/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
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
        JPA.withTransaction(() -> {
            TestEntity entity = TestEntity.find(1L);
            assertThat(entity.name, equalTo("test1"));
        });

        JPA.withTransaction(() -> {
            TestEntity entity = new TestEntity();
            entity.id = 2L;
            entity.name = "test2";
            entity.save();
        });

        JPA.withTransaction(() -> {
            TestEntity entity = TestEntity.find(2L);
            assertThat(entity.name, equalTo("test2"));
        });

        JPA.withTransaction(() -> {
            List<String> names = TestEntity.allNames();
            assertThat(names.size(), equalTo(2));
            assertThat(names, hasItems("test1", "test2"));
        });
    }

    @Test
    public void nestTransactions() {
        JPA.withTransaction(() -> {
            TestEntity entity = new TestEntity();
            entity.id = 2L;
            entity.name = "test2";
            entity.save();

            JPA.withTransaction(() -> {
                TestEntity entity2 = TestEntity.find(2L);
                assertThat(entity2, nullValue());
            });

            // Verify that we can still access the EntityManager
            TestEntity entity3 = TestEntity.find(2L);
            assertThat(entity3, equalTo(entity));
        });
    }

    @Test
    public void nestTransactionInnerRollback() {
        JPA.withTransaction(() -> {
            // Parent transaction creates entity 2
            TestEntity entity = createTestEntity(2L);

            JPA.withTransaction(() -> {
                // Nested transaction creates entity 3, but rolls back
                TestEntity entity2 = createTestEntity(3L);

                JPA.em().getTransaction().setRollbackOnly();
            });

            // Verify that we can still access the EntityManager
            TestEntity entity3 = TestEntity.find(2L);
            assertThat(entity3, equalTo(entity));
        });

        JPA.withTransaction(() -> {
            TestEntity entity = TestEntity.find(3L);
            assertThat(entity, nullValue());

            TestEntity entity2 = TestEntity.find(2L);
            assertThat(entity2.name, equalTo("test2"));
        });
    }

    @Test
    public void nestTransactionOuterRollback() {
        JPA.withTransaction(() -> {
            // Parent transaction creates entity 2, but rolls back
            TestEntity entity = createTestEntity(2L);

            JPA.withTransaction(() -> {
                // Nested transaction creates entity 3
                TestEntity entity2 = createTestEntity(3L);
            });

            // Verify that we can still access the EntityManager
            TestEntity entity3 = TestEntity.find(2L);
            assertThat(entity3, equalTo(entity));

            JPA.em().getTransaction().setRollbackOnly();
        });

        JPA.withTransaction(() -> {
            TestEntity entity = TestEntity.find(3L);
            assertThat(entity.name, equalTo("test3"));

            TestEntity entity2 = TestEntity.find(2L);
            assertThat(entity2, nullValue());
        });
    }

    private static TestEntity createTestEntity(long id) {
        TestEntity entity = new TestEntity();
        entity.id = id;
        entity.name = "test" + id;
        entity.save();
        return entity;
    }
}
