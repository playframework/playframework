/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import java.util.function.Consumer;
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
     * Get a newly created EntityManager for the specified persistence unit name.
     *
     * @param name The persistence unit name
     * @return EntityManager for the specified persistence unit name
     */
    public EntityManager em(String name);

    /**
     * Get the EntityManager for a particular persistence unit for this thread.
     *
     * @return EntityManager for the specified persistence unit name
     *
     * @deprecated The EntityManager is supplied as lambda parameter instead when using {@link #withTransaction(Function)}
     */
    @Deprecated
    public EntityManager em();

    /**
     * Run a block of code with a newly created EntityManager for the default Persistence Unit.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(Function<EntityManager, T> block);

    /**
     * Run a block of code with a newly created EntityManager for the default Persistence Unit.
     *
     * @param block Block of code to execute
     */
    public void withTransaction(Consumer<EntityManager> block);

    /**
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(String name, Function<EntityManager, T> block);

    /**
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param block Block of code to execute
     */
    public void withTransaction(String name, Consumer<EntityManager> block);

    /**
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     */
    public <T> T withTransaction(String name, boolean readOnly, Function<EntityManager, T> block);

    /**
     * Run a block of code with a newly created EntityManager for the named Persistence Unit.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     */
    public void withTransaction(String name, boolean readOnly, Consumer<EntityManager> block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     *
     * @deprecated Use {@link #withTransaction(Function)}
     */
    @Deprecated
    public <T> T withTransaction(Supplier<T> block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param block Block of code to execute
     *
     * @deprecated Use {@link #withTransaction(Consumer)}
     */
    @Deprecated
    public void withTransaction(Runnable block);

    /**
     * Run a block of code in a JPA transaction.
     *
     * @param name The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute
     * @param <T> type of result
     * @return code execution result
     *
     * @deprecated Use {@link #withTransaction(String, boolean, Function)}
     */
    @Deprecated
    public <T> T withTransaction(String name, boolean readOnly, Supplier<T> block);

    /**
     * Close all entity manager factories.
     */
    public void shutdown();
}
