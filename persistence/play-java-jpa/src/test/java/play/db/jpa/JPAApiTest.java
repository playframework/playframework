/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import static org.junit.jupiter.api.Assertions.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.*;
import play.db.Database;
import play.db.Databases;
import play.db.jpa.DefaultJPAConfig.JPAConfigProvider;

public class JPAApiTest {
  @RegisterExtension TestDatabaseExtension db = new TestDatabaseExtension();

  @Test
  public void shouldWorkWithEmptyConfiguration() {
    String configString = "";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertTrue(unitNames.isEmpty());
  }

  private Set<String> getConfiguredPersistenceUnitNames(String configString) {
    Config overrides = ConfigFactory.parseString(configString);
    Config config = overrides.withFallback(ConfigFactory.load());
    return new JPAConfigProvider(config)
        .get().persistenceUnits().stream().map(unit -> unit.unitName).collect(Collectors.toSet());
  }

  @Test
  public void shouldWorkWithSingleValue() {
    String configString = "jpa.default = defaultPersistenceUnit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertEquals(unitNames, new HashSet<>(List.of("defaultPersistenceUnit")));
  }

  @Test
  public void shouldWorkWithMultipleValues() {
    String configString = "jpa.default = defaultPersistenceUnit\n" + "jpa.number2 = number2Unit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertEquals(unitNames, new HashSet<>(Arrays.asList("defaultPersistenceUnit", "number2Unit")));
  }

  @Test
  public void shouldWorkWithEmptyConfigurationAtConfiguredLocation() {
    String configString = "play.jpa.config = myconfig.jpa";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertTrue(unitNames.isEmpty());
  }

  @Test
  public void shouldWorkWithSingleValueAtConfiguredLocation() {
    String configString =
        "play.jpa.config = myconfig.jpa\n" + "myconfig.jpa.default = defaultPersistenceUnit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertEquals(new HashSet<>(List.of("defaultPersistenceUnit")), unitNames);
  }

  @Test
  public void shouldWorkWithMultipleValuesAtConfiguredLocation() {
    String configString =
        "play.jpa.config = myconfig.jpa\n"
            + "myconfig.jpa.default = defaultPersistenceUnit\n"
            + "myconfig.jpa.number2 = number2Unit";
    Set<String> unitNames = getConfiguredPersistenceUnitNames(configString);
    assertEquals(unitNames, new HashSet<>(Arrays.asList("defaultPersistenceUnit", "number2Unit")));
  }

  @Test
  public void shouldBeAbleToGetAnEntityManagerWithAGivenName() {
    EntityManager em = db.jpa.em("default");
    assertNotNull(em);
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
          assertEquals("alice", entity.name);
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
          assertEquals("alice", entity.name);
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
          assertNull(entity);
        });
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
          assertEquals("alice", entity.name);
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
                assertNull(entity2);
              });

          // Verify that we can still access the EntityManager
          TestEntity entity3 = TestEntity.find(2L, entityManager);
          assertEquals(entity, entity3);
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
          assertEquals(entity, entity3);
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(3L, entityManager);
          assertNull(entity);

          TestEntity entity2 = TestEntity.find(2L, entityManager);
          assertEquals("alice", entity2.name);
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
          assertEquals(entity, entity3);

          entityManager.getTransaction().setRollbackOnly();
        });

    db.jpa.withTransaction(
        entityManager -> {
          TestEntity entity = TestEntity.find(3L, entityManager);
          assertEquals("alice", entity.name);

          TestEntity entity2 = TestEntity.find(2L, entityManager);
          assertNull(entity2);
        });
  }

  public static class TestDatabaseExtension implements BeforeEachCallback, AfterEachCallback {
    Database database;
    JPAApi jpa;

    public void execute(final String sql) {
      database.withConnection(
          connection -> {
            connection.createStatement().execute(sql);
          });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
      jpa.shutdown();
      database.shutdown();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
      database = Databases.inMemoryWith("jndiName", "DefaultDS");
      execute("create table TestEntity (id bigint not null, name varchar(255));");
      jpa = new DefaultJPAApi(DefaultJPAConfig.of("default", "defaultPersistenceUnit")).start();
    }
  }
}
