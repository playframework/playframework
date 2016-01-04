/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.*;
import play.libs.F;
import play.mvc.Http;

import java.util.function.Supplier;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.persistence.*;

/**
 * JPA Helpers.
 */
public class JPA {

    // Only used when there's no HTTP context
    static ThreadLocal<Deque<EntityManager>> currentEntityManager = new JPATransactionContext();

    private static class JPATransactionContext extends ThreadLocal<Deque<EntityManager>> {
        @Override
        protected Deque<EntityManager> initialValue() {
            return new ArrayDeque<>();
        }
    }

    private static final String CURRENT_ENTITY_MANAGER = "currentEntityManager";

    /**
     * Create a default JPAApi with the given persistence unit configuration.
     * Automatically initialise the JPA entity manager factories.
     *
     * @param name the EntityManagerFactory's name
     * @param unitName the persistence unit's name
     * @return the configured JPAApi
     */
    public static JPAApi createFor(String name, String unitName) {
        return new DefaultJPAApi(DefaultJPAConfig.of(name, unitName)).start();
    }

    /**
     * Create a default JPAApi with name "default" and the given unit name.
     * Automatically initialise the JPA entity manager factories.
     *
     * @param unitName the persistence unit's name
     * @return the configured JPAApi
     */
    public static JPAApi createFor(String unitName) {
        return new DefaultJPAApi(DefaultJPAConfig.of("default", unitName)).start();
    }

    /**
     * Get JPA api for the current play application.
     *
     * @return the JPAApi
     */
    public static JPAApi jpaApi() {
        Application app = Play.application();
        if (app == null) {
            throw new RuntimeException("No application running");
        }
        return app.injector().instanceOf(JPAApi.class);
    }

    /**
     * Get the EntityManager stack.
     */
    @SuppressWarnings("unchecked")
    private static Deque<EntityManager> emStack(boolean threadLocalFallback) {
        Http.Context context = Http.Context.current.get();
        if (context != null) {
            Object emsObject = context.args.get(CURRENT_ENTITY_MANAGER);
            if (emsObject != null) {
                return (Deque<EntityManager>) emsObject;
            } else {
                Deque<EntityManager> ems = new ArrayDeque<>();
                context.args.put(CURRENT_ENTITY_MANAGER, ems);
                return ems;
            }
        } else {
            // Not a web request
            if (threadLocalFallback) {
                return currentEntityManager.get();
            } else {
                throw new RuntimeException("No Http.Context is present. If you want to invoke this method outside of a HTTP request, you need to wrap the call with JPA.withTransaction instead.");
            }
        }
    }

    /**
     * Pushes or pops the EntityManager stack depending on the value of the
     * em argument. If em is null, then the current EntityManager is popped. If em
     * is non-null, then em is pushed onto the stack and becomes the current EntityManager.
     */
    private static void pushOrPopEm(EntityManager em, boolean threadLocalFallback) {
        Deque<EntityManager> ems = emStack(threadLocalFallback);
        if (em != null) {
            ems.push(em);
        } else {
            if (ems.isEmpty()) {
                throw new IllegalStateException("Tried to remove the EntityManager, but none was set.");
            }
            ems.pop();
        }
    }

    /**
     * Get the EntityManager for a particular persistence unit for this thread.
     *
     * @param key name of the EntityManager to return
     * @return the EntityManager
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
     *
     * @throws RuntimeException if no EntityManager is bound to the current Http.Context or the current Thread.
     * @return the EntityManager
     */
    public static EntityManager em() {
        Http.Context context = Http.Context.current.get();
        Deque<EntityManager> ems = emStack(true);

        if (ems.isEmpty()) {
            if (context != null) {
                throw new RuntimeException("No EntityManager found in the context. Try to annotate your action method with @play.db.jpa.Transactional");
            } else {
                throw new RuntimeException("No EntityManager bound to this thread. Try wrapping this call in JPA.withTransaction, or ensure that the HTTP context is setup on this thread.");
            }
        }

        return ems.peekFirst();
    }


    /**
     * Bind an EntityManager to the current HTTP context.
     * If no HTTP context is available the EntityManager gets bound to the current thread instead.
     *
     * @param em the EntityManager to bind to this HTTP context.
     */
    public static void bindForSync(EntityManager em) {
        pushOrPopEm(em, true);
    }

    /**
     * Bind an EntityManager to the current HTTP context.
     *
     * @param em the EntityManager to bind
     * @throws RuntimeException if no HTTP context is present.
     */
    public static void bindForAsync(EntityManager em) {
        pushOrPopEm(em, false);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     * @param <T> return type of the block
     * @return the result of the block, having already committed the transaction (or rolled it back in case of exception)
     */
    public static <T> T withTransaction(Supplier<T> block) {
        return jpaApi().withTransaction(block);
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute.
     * @param <T> return type of the block
     * @return a future to the result of the block, having already committed the transaction
     *         (or rolled it back in case of exception)
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public static <T> F.Promise<T> withTransactionAsync(Supplier<F.Promise<T>> block) {
        return jpaApi().withTransactionAsync(block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute.
     */
    public static void withTransaction(final Runnable block) {
        jpaApi().withTransaction(block);
    }

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     * @param <T> return type of the provided block
     * @return a future to the result of the block, having already committed the transaction
     *         (or rolled it back in case of exception)
     */
    public static <T> T withTransaction(String name, boolean readOnly, Supplier<T> block) {
        return jpaApi().withTransaction(name, readOnly, block);
    }

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     * @param <T> return type of the block
     * @return a future to the result of the block, having already committed the transaction
     *         (or rolled it back in case of exception)
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public static <T> F.Promise<T> withTransactionAsync(String name, boolean readOnly, Supplier<F.Promise<T>> block) {
        return jpaApi().withTransactionAsync(name, readOnly, block);
    }
}
