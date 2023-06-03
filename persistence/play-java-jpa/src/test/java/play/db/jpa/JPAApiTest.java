/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import play.db.Database;
import play.db.Databases;
import play.db.jpa.DefaultJPAConfig.JPAConfigProvider;

public class JPAApiTest {

  @Rule public TestDatabase db = new TestDatabase();

  private Set<String> getConfiguredPersistenceUnitNames(String configString) {
    Config overrides = ConfigFactory.parseString(configString);
    Config config = overrides.withFallback(ConfigFactory.load());
    return new JPAConfigProvider(config)
        .get().persistenceUnits().stream().map(unit -> unit.unitName).collect(Collectors.toSet());
  }

  @Test
  public void shouldWorkWithEmptyConfiguration() {
    String configString = "";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertThat(unitNames, equalTo(Collections.emptySet()));
  }

  @Test
  public void shouldWorkWithSingleValue() {
    String configString = "jpa.default = defaultPersistenceUnit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertThat(unitNames, equalTo(new HashSet<>(Arrays.asList("defaultPersistenceUnit"))));
  }

  @Test
  public void shouldWorkWithMultipleValues() {
    String configString = "jpa.default = defaultPersistenceUnit\n" + "jpa.number2 = number2Unit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertThat(
        unitNames, equalTo(new HashSet<>(Arrays.asList("defaultPersistenceUnit", "number2Unit"))));
  }

  @Test
  public void shouldWorkWithEmptyConfigurationAtConfiguredLocation() {
    String configString = "play.jpa.config = myconfig.jpa";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertThat(unitNames, equalTo(Collections.emptySet()));
  }

  @Test
  public void shouldWorkWithSingleValueAtConfiguredLocation() {
    String configString =
        "play.jpa.config = myconfig.jpa\n" + "myconfig.jpa.default = defaultPersistenceUnit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertThat(unitNames, equalTo(new HashSet<>(Arrays.asList("defaultPersistenceUnit"))));
  }

  @Test
  public void shouldWorkWithMultipleValuesAtConfiguredLocation() {
    String configString =
        "play.jpa.config = myconfig.jpa\n"
            + "myconfig.jpa.default = defaultPersistenceUnit\n"
            + "myconfig.jpa.number2 = number2Unit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertThat(
        unitNames, equalTo(new HashSet<>(Arrays.asList("defaultPersistenceUnit", "number2Unit"))));
  }

  @Test
  public void shouldBeAbleToGetAnEntityManagerWithAGivenName() {
    EntityManager em = db.jpa.em("default");
    assertThat(em, notNullValue());
  }

  @Test
  public void shouldExecuteAFunctionBlockUsingAEntityManager() {
    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = createTestEntity();
          entityManager.persist(entity);
          return entity;
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(1L, entityManager);
          assertThat(entity.name, equalTo("alice"));
        });
  }

  @Test
  public void shouldExecuteAFunctionBlockUsingASpecificNamedEntityManager() {
    db.jpa.withTransaction(
        "default",
        entityManager -> {
          TestEntity entity = createTestEntity();
          entityManager.persist(entity);
          return entity;
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(1L, entityManager);
          assertThat(entity.name, equalTo("alice"));
        });
  }

  @Test
  public void shouldExecuteAFunctionBlockAsAReadOnlyTransaction() {
    db.jpa.withTransaction(
        "default",
        true,
        entityManager -> {
          TestEntity entity = createTestEntity();
          entityManager.persist(entity);
          return entity;
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(1L, entityManager);
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
    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = createTestEntity();
          entity.save(entityManager);
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(1L, entityManager);
          assertThat(entity.name, equalTo("alice"));
        });
  }

  @Test
  public void shouldNestTransactions() {
    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = new TestEntity();
          entity.id = 2L;
          entity.name = "test2";
          entity.save(entityManager);

          db.jpa.withTransaction(
              entityManagerInner -> {
                TestEntity entity2 = TestEntity.find(2L, entityManagerInner);
                assertThat(entity2, nullValue());
              });

          // Verify that we can still access the EntityManager
          TestEntity entity3 = TestEntity.find(2L, entityManager);
          assertThat(entity3, equalTo(entity));
        });
  }

  @Test
  public void shouldRollbackInnerTransactionOnly() {
    db.jpa.withTransaction(
        entityManager -> {
          // Parent transaction creates entity 2
          TestEntity entity = createTestEntity(2L);
          entity.save(entityManager);

          db.jpa.withTransaction(
              entityManagerInner -> {
                // Nested transaction creates entity 3, but rolls back
                TestEntity entity2 = createTestEntity(3L);
                entity2.save(entityManagerInner);

                entityManagerInner.getTransaction().setRollbackOnly();
              });

          // Verify that we can still access the EntityManager
          TestEntity entity3 = TestEntity.find(2L, entityManager);
          assertThat(entity3, equalTo(entity));
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(3L, entityManager);
          assertThat(entity, nullValue());

          TestEntity entity2 = TestEntity.find(2L, entityManager);
          assertThat(entity2.name, equalTo("alice"));
        });
  }

  @Test
  public void shouldRollbackOuterTransactionOnly() {
    db.jpa.withTransaction(
        entityManager -> {
          // Parent transaction creates entity 2, but rolls back
          TestEntity entity = createTestEntity(2L);
          entity.save(entityManager);

          db.jpa.withTransaction(
              entityManagerInner -> {
                // Nested transaction creates entity 3
                TestEntity entity2 = createTestEntity(3L);
                entity2.save(entityManagerInner);
              });

          // Verify that we can still access the EntityManager
          TestEntity entity3 = TestEntity.find(2L, entityManager);
          assertThat(entity3, equalTo(entity));

          entityManager.getTransaction().setRollbackOnly();
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(3L, entityManager);
          assertThat(entity.name, equalTo("alice"));

          TestEntity entity2 = TestEntity.find(2L, entityManager);
          assertThat(entity2, nullValue());
        });
  }

  public static class TestDatabase extends ExternalResource {
    Database database;
    JPAApi jpa;

    public void execute(final String sql) {
      database.withConnection(
          connection -> {
            connection.createStatement().execute(sql);
          });
    }

    @Override
    public void before() {
      database = Databases.inMemoryWith("jndiName", "DefaultDS");
      execute("create table TestEntity (id bigint not null, name varchar(255));");
      jpa = new DefaultJPAApi(DefaultJPAConfig.of("default", "defaultPersistenceUnit")).start();
    }

    @Override
    public void after() {
      jpa.shutdown();
      database.shutdown();
    }
  }
}
