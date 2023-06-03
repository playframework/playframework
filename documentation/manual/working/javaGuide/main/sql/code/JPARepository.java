/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.sql;

// #jpa-repository-api-inject
import jakarta.persistence.*;
import java.util.concurrent.*;
import javax.inject.*;
import play.db.jpa.JPAApi;

@Singleton
public class JPARepository {
  private JPAApi jpaApi;
  private DatabaseExecutionContext executionContext;

  @Inject
  public JPARepository(JPAApi api, DatabaseExecutionContext executionContext) {
    this.jpaApi = api;
    this.executionContext = executionContext;
  }
}
// #jpa-repository-api-inject

class JPARepositoryMethods {
  private JPAApi jpaApi;
  private DatabaseExecutionContext executionContext;

  @Inject
  public JPARepositoryMethods(JPAApi api, DatabaseExecutionContext executionContext) {
    this.jpaApi = api;
    this.executionContext = executionContext;
  }

  // #jpa-withTransaction-function
  public CompletionStage<Long> runningWithTransaction() {
    return CompletableFuture.supplyAsync(
        () -> {
          // lambda is an instance of Function<EntityManager, Long>
          return jpaApi.withTransaction(
              entityManager -> {
                Query query = entityManager.createNativeQuery("select max(age) from people");
                return (Long) query.getSingleResult();
              });
        },
        executionContext);
  }
  // #jpa-withTransaction-function

  // #jpa-withTransaction-consumer
  public CompletionStage<Void> runningWithRunnable() {
    // lambda is an instance of Consumer<EntityManager>
    return CompletableFuture.runAsync(
        () -> {
          jpaApi.withTransaction(
              entityManager -> {
                Query query =
                    entityManager.createNativeQuery("update people set active = 1 where age > 18");
                query.executeUpdate();
              });
        },
        executionContext);
  }
  // #jpa-withTransaction-consumer
}
