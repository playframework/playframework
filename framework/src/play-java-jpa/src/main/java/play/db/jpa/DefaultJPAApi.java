/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.*;
import play.inject.ApplicationLifecycle;
import play.libs.F;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.*;

/**
 * Default implementation of the JPA API.
 */
@Singleton
public class DefaultJPAApi implements JPAApi {

    private final JPAConfig jpaConfig;

    private final Map<String, EntityManagerFactory> emfs = new HashMap<String, EntityManagerFactory>();

    @Inject
    public DefaultJPAApi(JPAConfig jpaConfig, ApplicationLifecycle lifecycle) {
        this.jpaConfig = jpaConfig;
        lifecycle.addStopHook(new Callable<F.Promise<Void>>() {
            @Override
            public F.Promise<Void> call() throws Exception {
                stop();
                return F.Promise.pure(null);
            }
        });
        start();
    }

    /**
     * Initialises required JPA entity manager factories.
     */
    private void start() {
        for (JPAConfig.PersistenceUnit persistenceUnit: jpaConfig.persistenceUnits()) {
            emfs.put(persistenceUnit.name, Persistence.createEntityManagerFactory(persistenceUnit.unitName));
        }
    }

    public EntityManager em(String key) {
        EntityManagerFactory emf = emfs.get(key);
        if (emf == null) {
            return null;
        }
        return emf.createEntityManager();
    }

    private void stop() {
        for (EntityManagerFactory emf: emfs.values()) {
            emf.close();
        }
    }

}
