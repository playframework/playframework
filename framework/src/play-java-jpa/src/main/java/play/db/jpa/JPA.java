/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.*;
import play.libs.F;
import play.mvc.Http;

import javax.persistence.*;

/**
 * JPA Helpers.
 */
public class JPA {

    // Only used when there's no HTTP context
    static ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<EntityManager>();

    private static final String CURRENT_ENTITY_MANAGER = "currentEntityManager";

    /**
     * Create a default JPAApi with the given persistence unit configuration.
     * Automatically initialise the JPA entity manager factories.
     */
    public static JPAApi createFor(String name, String unitName) {
        return new DefaultJPAApi(DefaultJPAConfig.of(name, unitName)).start();
    }

    /**
     * Create a default JPAApi with name "default" and the given unit name.
     * Automatically initialise the JPA entity manager factories.
     */
    public static JPAApi createFor(String unitName) {
        return new DefaultJPAApi(DefaultJPAConfig.of("default", unitName)).start();
    }

    /**
     * Get the JPA api for the current play application.
     */
    public static JPAApi jpaApi() {
        Application app = Play.application();
        if (app == null) {
            throw new RuntimeException("No application running");
        }
        return app.injector().instanceOf(JPAApi.class);
    }

    /**
     * Get the EntityManager for specified persistence unit for this thread.
     */
    public static EntityManager em(String key) {
        EntityManager em = jpaApi().em(key);
        if (em == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }

        return em;
    }

    /**
     * Get the default EntityManager for this thread.
     */
    public static EntityManager em() {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            EntityManager em = (EntityManager) context.args.get(CURRENT_ENTITY_MANAGER);
            if (em == null) {
                throw new RuntimeException("No EntityManager found in the context. Try to annotate your action method with @play.db.jpa.Transactional");
            }
            return em;
        }
        // Not a web request
        EntityManager em = currentEntityManager.get();
        if(em == null) {
            throw new RuntimeException("No EntityManager bound to this thread. Try wrapping this call in JPA.withTransaction, or ensure that the HTTP context is setup on this thread.");
        }
        return em;
    }

    /**
     * Bind an EntityManager to the current HTTP context.
     * If no HTTP context is available the EntityManager gets bound to the current thread instead.
     */
    public static void bindForSync(EntityManager em) {
        bindForCurrentContext(em, true);
    }

    /**
     * Bind an EntityManager to the current HTTP context.
     *
     * @throws RuntimeException if no HTTP context is present.
     */
    public static void bindForAsync(EntityManager em) {
        bindForCurrentContext(em, false);
    }

    /**
     * Bind an EntityManager to the current HTTP context.
     *
     * @throws RuntimeException if no HTTP context is present and {@code threadLocalFallback} is false.
     */
    private static void bindForCurrentContext(EntityManager em, boolean threadLocalFallback) {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            if (em == null) {
                context.args.remove(CURRENT_ENTITY_MANAGER);
            } else {
                context.args.put(CURRENT_ENTITY_MANAGER, em);
            }
        } else {
            // Not a web request
            if(threadLocalFallback) {
                currentEntityManager.set(em);
            } else {
                throw new RuntimeException("No Http.Context is present. If you want to invoke this method outside of a HTTP request, you need to wrap the call with JPA.withTransaction instead.");
            }
        }
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static <T> T withTransaction(play.libs.F.Function0<T> block) throws Throwable {
        return jpaApi().withTransaction(block);
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute.
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public static <T> F.Promise<T> withTransactionAsync(play.libs.F.Function0<F.Promise<T>> block) throws Throwable {
        return jpaApi().withTransactionAsync(block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static void withTransaction(final play.libs.F.Callback0 block) {
        jpaApi().withTransaction(block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     */
    public static <T> T withTransaction(String name, boolean readOnly, play.libs.F.Function0<T> block) throws Throwable {
        return jpaApi().withTransaction(name, readOnly, block);
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
    public static <T> F.Promise<T> withTransactionAsync(String name, boolean readOnly, play.libs.F.Function0<F.Promise<T>> block) throws Throwable {
        return jpaApi().withTransactionAsync(name, readOnly, block);
    }
}
