/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import play.Configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Default JPA configuration.
 */
public class DefaultJPAConfig implements JPAConfig {

    private Set<JPAConfig.PersistenceUnit> persistenceUnits;

    public DefaultJPAConfig(Set<JPAConfig.PersistenceUnit> persistenceUnits) {
        this.persistenceUnits = persistenceUnits;
    }

    @Override
    public Set<JPAConfig.PersistenceUnit> persistenceUnits() {
        return persistenceUnits;
    }

    @Singleton
    public static class JPAConfigProvider implements Provider<JPAConfig> {
        private final JPAConfig jpaConfig;

        @Inject
        public JPAConfigProvider(Configuration configuration) {
            Set<JPAConfig.PersistenceUnit> persistenceUnits = new HashSet<JPAConfig.PersistenceUnit>();
            Configuration jpa = configuration.getConfig("jpa");
            if (jpa != null) {
                for (String name : jpa.keys()) {
                    String unitName = jpa.getString(name);
                    persistenceUnits.add(new JPAConfig.PersistenceUnit(name, unitName));
                }
            }
            jpaConfig = new DefaultJPAConfig(persistenceUnits);
        }

        @Override
        public JPAConfig get() {
            return jpaConfig;
        }
    }

    public static JPAConfig from(Map<String, String> map) {
        Set<JPAConfig.PersistenceUnit> persistenceUnits = new HashSet<JPAConfig.PersistenceUnit>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            persistenceUnits.add(new JPAConfig.PersistenceUnit(entry.getKey(), entry.getValue()));
        }
        return new DefaultJPAConfig(persistenceUnits);
    }
}
