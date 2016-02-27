/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db.jpa;

import javax.persistence.EntityManager;
import java.util.function.Supplier;

/**
 * JPA Helpers.
 */
public class JPA {

    static JPAEntityManagerContext entityManagerContext = new JPAEntityManagerContext();

    /**
     * Create a default JPAApi with the given persistence unit configuration.
     * Automatically initialise the JPA entity manager factories.
     *
     * @param name the EntityManagerFactory's name
     * @param unitName the persistence unit's name
     * @return the configured JPAApi
     */
    public static JPAApi createFor(String name, String unitName) {
        return new DefaultJPAApi(DefaultJPAConfig.of(name, unitName), entityManagerContext).start();
    }

    /**
     * Create a default JPAApi with name "default" and the given unit name.
     * Automatically initialise the JPA entity manager factories.
     *
     * @param unitName the persistence unit's name
     * @return the configured JPAApi
     */
    public static JPAApi createFor(String unitName) {
        return new DefaultJPAApi(DefaultJPAConfig.of("default", unitName), entityManagerContext).start();
    }

    /**
     * Get JPA api for the current play application.
     *
     * @deprecated as of 2.5.0. Inject a JPAApi instead.
     *
     * @return the JPAApi
     */
    @Deprecated
    public static JPAApi jpaApi() {
        JPAConfig jpaConfig = new DefaultJPAConfig.JPAConfigProvider(play.Play.application().configuration()).get();
        return new DefaultJPAApi(jpaConfig, entityManagerContext).start();
    }

    /**
     * Get the EntityManager for a particular persistence unit for this thread.
     *
     * @param key name of the EntityManager to return
     * @return the EntityManager
     */
    @Deprecated
    public static EntityManager em(String key) {
        EntityManager em = jpaApi().em(key);
        if (em == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }

        return em;
    }

    /**
     * Get the default EntityManager for this thread.
     *
     * @throws RuntimeException if no EntityManager is bound to the current Http.Context or the current Thread.
     * @return the EntityManager
     */
    public static EntityManager em() {
        return entityManagerContext.em();
    }


    /**
     * Bind an EntityManager to the current HTTP context.
     * If no HTTP context is available the EntityManager gets bound to the current thread instead.
     *
     * @param em the EntityManager to bind to this HTTP context.
     */
    public static void bindForSync(EntityManager em) {
        entityManagerContext.pushOrPopEm(em, true);
    }

    /**
     * Bind an EntityManager to the current HTTP context.
     *
     * @param em the EntityManager to bind
     * @throws RuntimeException if no HTTP context is present.
     *
     * @deprecated Use JPAEntityManagerContext.push or JPAEntityManagerContext.pop
     */
    @Deprecated
    public static void bindForAsync(EntityManager em) {
        entityManagerContext.pushOrPopEm(em, false);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @deprecated as of 2.5.0. Inject a JPAApi instead.
     *
     * @param block Block of code to execute.
     * @param <T> return type of the block
     * @return the result of the block, having already committed the transaction (or rolled it back in case of exception)
     */
    @Deprecated
    public static <T> T withTransaction(Supplier<T> block) {
        return jpaApi().withTransaction(block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @deprecated as of 2.5.0. Inject a JPAApi instead.
     *
     * @param block Block of code to execute.
     */
    @Deprecated
    public static void withTransaction(final Runnable block) {
        jpaApi().withTransaction(block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @deprecated as of 2.5.0. Inject a JPAApi instead.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     * @param <T> return type of the provided block
     * @return a future to the result of the block, having already committed the transaction
     *         (or rolled it back in case of exception)
     */
    @Deprecated
    public static <T> T withTransaction(String name, boolean readOnly, Supplier<T> block) {
        return jpaApi().withTransaction(name, readOnly, block);
    }
}
