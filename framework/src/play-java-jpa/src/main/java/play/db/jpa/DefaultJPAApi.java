/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.db.DBApi;
import play.inject.ApplicationLifecycle;
import play.libs.F;

import java.util.*;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.*;

/**
 * Default implementation of the JPA API.
 */
public class DefaultJPAApi implements JPAApi {

    private final JPAConfig jpaConfig;

    private final Map<String, EntityManagerFactory> emfs = new HashMap<String, EntityManagerFactory>();

    public DefaultJPAApi(JPAConfig jpaConfig) {
        this.jpaConfig = jpaConfig;
    }

    @Singleton
    public static class JPAApiProvider implements Provider<JPAApi> {
        private final JPAApi jpaApi;

        @Inject
        public JPAApiProvider(JPAConfig jpaConfig, DBApi dbApi, ApplicationLifecycle lifecycle) {
            // dependency on db api ensures that the databases are initialised
            jpaApi = new DefaultJPAApi(jpaConfig);
            lifecycle.addStopHook(() -> {
                jpaApi.shutdown();
                return F.Promise.pure(null);
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
        for (JPAConfig.PersistenceUnit persistenceUnit : jpaConfig.persistenceUnits()) {
            emfs.put(persistenceUnit.name, Persistence.createEntityManagerFactory(persistenceUnit.unitName));
        }
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
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     */
    public <T> T withTransaction(Supplier<T> block) {
        return withTransaction("default", false, block);
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public <T> F.Promise<T> withTransactionAsync(Supplier<F.Promise<T>> block) {
        return withTransactionAsync("default", false, block);
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
        EntityManager entityManager = null;
        EntityTransaction tx = null;

        try {
            entityManager = em(name);

            if (entityManager == null) {
                throw new RuntimeException("No JPA entity manager defined for '" + name + "'");
            }

            JPA.bindForSync(entityManager);

            if (!readOnly) {
                tx = entityManager.getTransaction();
                tx.begin();
            }

            T result = block.get();

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
                try { tx.rollback(); } catch (Throwable e) {}
            }
            throw t;
        } finally {
            JPA.bindForSync(null);
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public <T> F.Promise<T> withTransactionAsync(String name, boolean readOnly, Supplier<F.Promise<T>> block) {
        EntityManager entityManager = null;
        EntityTransaction tx = null;

        try {
            entityManager = em(name);

            if (entityManager == null) {
                throw new RuntimeException("No JPA entity manager defined for '" + name + "'");
            }

            JPA.bindForAsync(entityManager);

            if (!readOnly) {
                tx = entityManager.getTransaction();
                tx.begin();
            }

            F.Promise<T> result = block.get();

            final EntityManager fem = entityManager;
            final EntityTransaction ftx = tx;

            F.Promise<T> committedResult = (ftx == null) ? result : result.map(t -> {
                if (ftx.getRollbackOnly()) {
                    ftx.rollback();
                } else {
                    ftx.commit();
                }
                return t;
            });

            committedResult.onFailure(t -> {
                if (ftx != null) {
                    try { if (ftx.isActive()) { ftx.rollback(); } } catch (Throwable e) {}
                }
                try {
                    fem.close();
                } finally {
                    JPA.bindForAsync(null);
                }
            });
            committedResult.onRedeem(t -> {
                try {
                    fem.close();
                } finally {
                    JPA.bindForAsync(null);
                }
            });

            return committedResult;

        } catch (Throwable t) {
            if (tx != null) {
                try { tx.rollback(); } catch (Throwable e) {}
            }
            if (entityManager != null) {
                try {
                    entityManager.close();
                } finally {
                    JPA.bindForAsync(null);
                }
            }
            throw t;
        }
    }

    /**
     * Close all entity manager factories.
     */
    public void shutdown() {
        for (EntityManagerFactory emf : emfs.values()) {
            emf.close();
        }
    }

}
