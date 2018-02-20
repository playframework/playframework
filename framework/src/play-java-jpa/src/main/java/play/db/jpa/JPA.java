/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import javax.persistence.EntityManager;

/**
 * JPA Helpers.  This is a deprecated class, and an injected JPAApi instance should be used instead.
 *
 * Please see <a href="https://www.playframework.com/documentation/latest/JavaJPA#Using-play.db.jpa.JPAApi">Using play.db.jpa.JPAApi</a> for more details.
 *
 * @deprecated Use a dependency injected JPAApi instance here, since 2.6.1
 */
@Deprecated
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

}
