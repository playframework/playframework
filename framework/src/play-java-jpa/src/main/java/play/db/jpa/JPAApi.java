/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

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
     * Get the EntityManager for a particular persistence unit for this thread.
     *
     * @return EntityManager for the specified persistence unit name
     */
    public EntityManager em();

    /**
     * Run a block of code with a given EntityManager.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(Function<EntityManager, T> block);

    /**
     * Run a block of code with a given EntityManager.
     *
     * @param name The persistence unit name
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(String name, Function<EntityManager, T> block);

    /**
     * Run a block of code with a given EntityManager.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(String name, boolean readOnly, Function<EntityManager, T> block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(Supplier<T> block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     */
    public void withTransaction(Runnable block);

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
     * Close all entity manager factories.
     */
    public void shutdown();
}
