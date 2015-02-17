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
            EntityManager em = (EntityManager) context.args.get("currentEntityManager");
            if (em == null) {
                throw new RuntimeException("No EntityManager bound to this thread. Try to annotate your action method with @play.db.jpa.Transactional");
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
     * Bind an EntityManager to the current thread.
     */
    public static void bindForCurrentThread(EntityManager em) {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            if (em == null) {
                context.args.remove("currentEntityManager");
            } else {
                context.args.put("currentEntityManager", em);
            }
        } else {
            currentEntityManager.set(em);
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
