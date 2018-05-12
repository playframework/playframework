/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.DBApi;
import play.inject.ApplicationLifecycle;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.*;

/**
 * Default implementation of the JPA API.
 */
public class DefaultJPAApi implements JPAApi {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJPAApi.class);

    private final JPAConfig jpaConfig;

    private final Map<String, EntityManagerFactory> emfs = new HashMap<>();

    private final JPAEntityManagerContext entityManagerContext;

    public DefaultJPAApi(JPAConfig jpaConfig, JPAEntityManagerContext entityManagerContext) {
        this.jpaConfig = jpaConfig;
        this.entityManagerContext = entityManagerContext;
    }

    @Singleton
    public static class JPAApiProvider implements Provider<JPAApi> {
        private final JPAApi jpaApi;

        @Inject
        public JPAApiProvider(JPAConfig jpaConfig, JPAEntityManagerContext context, ApplicationLifecycle lifecycle, DBApi dbApi) {
            // dependency on db api ensures that the databases are initialised
            jpaApi = new DefaultJPAApi(jpaConfig, context);
            lifecycle.addStopHook(() -> {
                jpaApi.shutdown();
                return CompletableFuture.completedFuture(null);
            });
            jpaApi.start();
        }

        @Override
        public JPAApi get() {
            return jpaApi;
        }
    }

    /**
     * Initialise JPA entity manager factories.
     */
    public JPAApi start() {
        jpaConfig.persistenceUnits().forEach(persistenceUnit ->
                emfs.put(persistenceUnit.name, Persistence.createEntityManagerFactory(persistenceUnit.unitName))
        );
        return this;
    }

    /**
     * Get the EntityManager for the specified persistence unit name.
     *
     * @param name The persistence unit name
     */
    public EntityManager em(String name) {
        EntityManagerFactory emf = emfs.get(name);
        if (emf == null) {
            return null;
        }
        return emf.createEntityManager();
    }

    /**
     * Get the EntityManager for a particular persistence unit for this thread.
     *
     * @return EntityManager for the specified persistence unit name
     */
    public EntityManager em() {
        return entityManagerContext.em();
    }

    /**
     * Run a block of code with the EntityManager for the named Persistence Unit.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(Function<EntityManager, T> block) {
        return withTransaction("default", false, block);
    }

    /**
     * Run a block of code with the EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(String name, Function<EntityManager, T> block) {
        return withTransaction(name, false, block);
    }

    /**
     * Run a block of code with the EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(String name, boolean readOnly, Function<EntityManager, T> block) {
        EntityManager entityManager = null;
        EntityTransaction tx = null;

        try {
            entityManager = em(name);

            if (entityManager == null) {
                throw new RuntimeException("No JPA entity manager defined for '" + name + "'");
            }

            entityManagerContext.push(entityManager, true);

            if (!readOnly) {
                tx = entityManager.getTransaction();
                tx.begin();
            }

            T result = block.apply(entityManager);

            if (tx != null) {
                if(tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }

            return result;

        } catch (Throwable t) {
            if (tx != null) {
                try {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                } catch (Exception e) {
                    logger.error("Could not rollback transaction", e);
                }
            }
            throw t;
        } finally {
            if (entityManager != null) {
                entityManagerContext.pop(true);
                entityManager.close();
            }
        }
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     */
    public <T> T withTransaction(Supplier<T> block) {
        return withTransaction("default", false, block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     */
    public void withTransaction(final Runnable block) {
        try {
            withTransaction("default", false, () -> {
                block.run();
                return null;
            });
        } catch (Throwable t) {
            throw new RuntimeException("JPA transaction failed", t);
        }
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     */
    public <T> T withTransaction(String name, boolean readOnly, Supplier<T> block) {
        return withTransaction(name, readOnly, entityManager -> {
            return block.get();
        });
    }

    /**
     * Close all entity manager factories.
     */
    public void shutdown() {
        emfs.values().forEach(EntityManagerFactory::close);
    }

}
