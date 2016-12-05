/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db.jpa;

import play.mvc.Http;

import javax.persistence.EntityManager;

/**
 * JPA Helpers.
 */
public class JPA {

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
     * Get the default EntityManager from the current Http.Context.
     *
     * @throws RuntimeException if no EntityManager is bound to the current Http.Context.
     * @return the EntityManager
     * 
     * @deprecated Use {@link #em(play.mvc.Http.Context)} instead
     */
    @Deprecated
    public static EntityManager em() {
        return JPAEntityManagerContext.em();
    }

    /**
     * Get the default EntityManager from the given Http.Context.
     *
     * @throws RuntimeException if no EntityManager is bound to the given Http.Context.
     * @return the EntityManager
     */
    public static EntityManager em(Http.Context ctx) {
        return JPAEntityManagerContext.em(ctx);
    }

}
