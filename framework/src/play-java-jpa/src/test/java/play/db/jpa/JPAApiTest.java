/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db.jpa;

import org.junit.Rule;
import org.junit.rules.ExternalResource;
import play.db.Database;
import play.db.Databases;

import org.junit.Test;

import javax.persistence.EntityManager;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class JPAApiTest {

    @Rule
    public TestDatabase db = new TestDatabase();

    @Test
    public void shouldBeAbleToGetAnEntityManagerWithAGivenName() {
        EntityManager em = db.jpa.em("default");
        assertThat(em, notNullValue());
    }

    @Test
    public void shouldExecuteAFunctionBlockUsingAEntityManager() {
        db.jpa.withTransaction(entityManager -> {
            TestEntity entity = createTestEntity();
            entityManager.persist(entity);
            return entity;
        });

        db.jpa.withTransaction(() -> {
            TestEntity entity = TestEntity.find(1L);
            assertThat(entity.name, equalTo("alice"));
        });
    }

    @Test
    public void shouldReuseEntityManagerWhenExecutingTransaction() {
        JPAApi api = db.jpa;
        boolean reused = api.withTransaction(entityManager -> {
            EntityManager fromContext = api.em();
            return fromContext == entityManager;
        });

        assertThat(reused, is(true));
    }

    @Test
    public void shouldExecuteAFunctionBlockUsingASpecificNamedEntityManager() {
        db.jpa.withTransaction("default", entityManager -> {
            TestEntity entity = createTestEntity();
            entityManager.persist(entity);
            return entity;
        });

        db.jpa.withTransaction(() -> {
            TestEntity entity = TestEntity.find(1L);
            assertThat(entity.name, equalTo("alice"));
        });
    }

    @Test
    public void shouldExecuteAFunctionBlockAsAReadOnlyTransaction() {
        db.jpa.withTransaction("default", true, entityManager -> {
            TestEntity entity = createTestEntity();
            entityManager.persist(entity);
            return entity;
        });

        db.jpa.withTransaction(() -> {
            TestEntity entity = TestEntity.find(1L);
            assertThat(entity, nullValue());
        });
    }

    private TestEntity createTestEntity() {
        return createTestEntity(1L);
    }

    private TestEntity createTestEntity(Long id) {
        TestEntity entity = new TestEntity();
        entity.id = id;
        entity.name = "alice";
        return entity;
    }

    @Test
    public void shouldExecuteASupplierBlockInsideATransaction() throws Exception {
        db.jpa.withTransaction(() -> {
            TestEntity entity = createTestEntity();
            entity.save();
        });

        db.jpa.withTransaction(() -> {
            TestEntity entity = TestEntity.find(1L);
            assertThat(entity.name, equalTo("alice"));
        });
    }

    @Test
    public void shouldNestTransactions() {
        db.jpa.withTransaction(() -> {
            TestEntity entity = new TestEntity();
            entity.id = 2L;
            entity.name = "test2";
            entity.save();

            db.jpa.withTransaction(() -> {
                TestEntity entity2 = TestEntity.find(2L);
                assertThat(entity2, nullValue());
            });

            // Verify that we can still access the EntityManager
            TestEntity entity3 = TestEntity.find(2L);
            assertThat(entity3, equalTo(entity));
        });
    }

    @Test
    public void shouldRollbackInnerTransactionOnly() {
        db.jpa.withTransaction(() -> {
            // Parent transaction creates entity 2
            TestEntity entity = createTestEntity(2L);
            entity.save();

            db.jpa.withTransaction(() -> {
                // Nested transaction creates entity 3, but rolls back
                TestEntity entity2 = createTestEntity(3L);
                entity2.save();

                JPA.em().getTransaction().setRollbackOnly();
            });

            // Verify that we can still access the EntityManager
            TestEntity entity3 = TestEntity.find(2L);
            assertThat(entity3, equalTo(entity));
        });

        db.jpa.withTransaction(() -> {
            TestEntity entity = TestEntity.find(3L);
            assertThat(entity, nullValue());

            TestEntity entity2 = TestEntity.find(2L);
            assertThat(entity2.name, equalTo("alice"));
        });
    }

    @Test
    public void shouldRollbackOuterTransactionOnly() {
        db.jpa.withTransaction(() -> {
            // Parent transaction creates entity 2, but rolls back
            TestEntity entity = createTestEntity(2L);
            entity.save();

            db.jpa.withTransaction(() -> {
                // Nested transaction creates entity 3
                TestEntity entity2 = createTestEntity(3L);
                entity2.save();
            });

            // Verify that we can still access the EntityManager
            TestEntity entity3 = TestEntity.find(2L);
            assertThat(entity3, equalTo(entity));

            db.jpa.em().getTransaction().setRollbackOnly();
        });

        db.jpa.withTransaction(() -> {
            TestEntity entity = TestEntity.find(3L);
            assertThat(entity.name, equalTo("alice"));

            TestEntity entity2 = TestEntity.find(2L);
            assertThat(entity2, nullValue());
        });
    }

    public static class TestDatabase extends ExternalResource {
        Database database;
        JPAApi jpa;

        public void execute(final String sql) {
            database.withConnection(connection -> {
                connection.createStatement().execute(sql);
            });
        }

        @Override
        public void before() {
            database = Databases.inMemoryWith("jndiName", "DefaultDS");
            execute("create table TestEntity (id bigint not null, name varchar(255));");
            jpa = JPA.createFor("defaultPersistenceUnit");
        }

        @Override
        public void after() {
            jpa.shutdown();
            database.shutdown();
        }
    }

}
