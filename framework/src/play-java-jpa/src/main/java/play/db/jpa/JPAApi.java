/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import javax.persistence.EntityManager;
import play.libs.F;

/**
 * JPA API.
 */
public interface JPAApi {

    /**
     * Initialise JPA entity manager factories.
     */
    public JPAApi start();

    /**
     * Get the EntityManager for the specified persistence unit name.
     *
     * @param name The persistence unit name
     */
    public EntityManager em(String name);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     */
    public <T> T withTransaction(play.libs.F.Function0<T> block) throws Throwable;

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public <T> F.Promise<T> withTransactionAsync(play.libs.F.Function0<F.Promise<T>> block) throws Throwable;

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     */
    public void withTransaction(final play.libs.F.Callback0 block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     */
    public <T> T withTransaction(String name, boolean readOnly, play.libs.F.Function0<T> block) throws Throwable;

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
    public <T> F.Promise<T> withTransactionAsync(String name, boolean readOnly, play.libs.F.Function0<F.Promise<T>> block) throws Throwable;

    /**
     * Close all entity manager factories.
     */
    public void shutdown();
}
