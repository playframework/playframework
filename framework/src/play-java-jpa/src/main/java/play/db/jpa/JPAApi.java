/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import java.util.function.Supplier;

import javax.persistence.EntityManager;
import play.libs.F;

/**
 * JPA API.
 */
public interface JPAApi {

    /**
     * Initialise JPA entity manager factories.
     *
     * @return JPAApi instance
     */
    public JPAApi start();

    /**
     * Get the EntityManager for the specified persistence unit name.
     *
     * @param name The persistence unit name
     * @return EntityManager for the specified persistence unit name
     */
    public EntityManager em(String name);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(Supplier<T> block);

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public <T> F.Promise<T> withTransactionAsync(Supplier<F.Promise<T>> block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     */
    public void withTransaction(final Runnable block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(String name, boolean readOnly, Supplier<T> block);

    /**
     * Run a block of asynchronous code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     * @param <T> type of result
     * @return code execution result
     *
     * @deprecated This may cause deadlocks
     */
    @Deprecated
    public <T> F.Promise<T> withTransactionAsync(String name, boolean readOnly, Supplier<F.Promise<T>> block);

    /**
     * Close all entity manager factories.
     */
    public void shutdown();
}
