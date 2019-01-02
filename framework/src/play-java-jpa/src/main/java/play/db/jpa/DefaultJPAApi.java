/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.DBApi;
import play.inject.ApplicationLifecycle;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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

    /**
     * @deprecated Deprecated as of 2.7.0. Use {@link #DefaultJPAApi(JPAConfig)} instead.
     */
    @Deprecated
    public DefaultJPAApi(JPAConfig jpaConfig, JPAEntityManagerContext entityManagerContext) {
        this.jpaConfig = jpaConfig;
        this.entityManagerContext = entityManagerContext;
    }

    public DefaultJPAApi(JPAConfig jpaConfig) {
        this(jpaConfig, null);
    }

    @Singleton
    public static class JPAApiProvider implements Provider<JPAApi> {
        private final JPAApi jpaApi;

        /**
         * @deprecated Deprecated as of 2.7.0. Use {@link #JPAApiProvider(JPAConfig, ApplicationLifecycle, DBApi, Config)} instead.
         */
        @Inject
        @Deprecated
        public JPAApiProvider(JPAConfig jpaConfig, JPAEntityManagerContext context, ApplicationLifecycle lifecycle, DBApi dbApi, Config config) {
            // dependency on db api ensures that the databases are initialised
            jpaApi = new DefaultJPAApi(jpaConfig, config.getBoolean("play.jpa.allowJPAEntityManagerContext") ? context : null);
            lifecycle.addStopHook(() -> {
                jpaApi.shutdown();
                return CompletableFuture.completedFuture(null);
            });
            jpaApi.start();
        }

        public JPAApiProvider(JPAConfig jpaConfig, ApplicationLifecycle lifecycle, DBApi dbApi, Config config) {
            this(jpaConfig, null, lifecycle, dbApi, config);
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
     * Get a newly created EntityManager for the specified persistence unit name.
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
     *
     * @deprecated Deprecated as of 2.7.0. The EntityManager is supplied as lambda parameter instead when using {@link #withTransaction(Function)}
     */
    @Deprecated
    public EntityManager em() {
        if(entityManagerContext == null) {
            throw new RuntimeException("EntityManager can't be acquired from a thread-local. You should instead use one of the JPAApi methods where the EntityManager is provided automatically.");
        }
        return entityManagerContext.em();
    }

    /**
     * Run a block of code with a newly created EntityManager for the default Persistence Unit.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(Function<EntityManager, T> block) {
        return withTransaction("default", block);
    }

    /**
     * Run a block of code with a newly created EntityManager for the default Persistence Unit.
     *
     * @param block Block of code to execute
     */
    public void withTransaction(Consumer<EntityManager> block) {
        withTransaction(em -> {
            block.accept(em);
            return null;
        });
    }

    /**
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
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
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param block Block of code to execute
     */
    public void withTransaction(String name, Consumer<EntityManager> block) {
        withTransaction(name, em -> {
            block.accept(em);
            return null;
        });
    }

    /**
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
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
                throw new RuntimeException("Could not create JPA entity manager for '" + name + "'");
            }

            if (entityManagerContext != null) {
                entityManagerContext.push(entityManager, true);
            }

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
                if (entityManagerContext != null) {
                    entityManagerContext.pop(true);
                }
                entityManager.close();
            }
        }
    }

    /**
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     */
    public void withTransaction(String name, boolean readOnly, Consumer<EntityManager> block) {
        withTransaction(name, readOnly, em -> {
            block.accept(em);
            return null;
        });
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #withTransaction(Function)} instead.
     */
    @Deprecated
    public <T> T withTransaction(Supplier<T> block) {
        return withTransaction("default", false, block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #withTransaction(Consumer)} instead.
     */
    @Deprecated
    public void withTransaction(final Runnable block) {
        try {
            withTransaction(() -> {
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
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #withTransaction(String, boolean, Function)} instead.
     */
    @Deprecated
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
