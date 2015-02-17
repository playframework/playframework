/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import play.Configuration;

import com.google.common.collect.ImmutableSet;

/**
 * Default JPA configuration.
 */
public class DefaultJPAConfig implements JPAConfig {

    private Set<JPAConfig.PersistenceUnit> persistenceUnits;

    public DefaultJPAConfig(Set<JPAConfig.PersistenceUnit> persistenceUnits) {
        this.persistenceUnits = persistenceUnits;
    }

    public DefaultJPAConfig(JPAConfig.PersistenceUnit... persistenceUnits) {
        this(ImmutableSet.copyOf(persistenceUnits));
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
            ImmutableSet.Builder<JPAConfig.PersistenceUnit> persistenceUnits = new ImmutableSet.Builder<JPAConfig.PersistenceUnit>();
            Configuration jpa = configuration.getConfig("jpa");
            if (jpa != null) {
                for (String name : jpa.keys()) {
                    String unitName = jpa.getString(name);
                    persistenceUnits.add(new JPAConfig.PersistenceUnit(name, unitName));
                }
            }
            jpaConfig = new DefaultJPAConfig(persistenceUnits.build());
        }

        @Override
        public JPAConfig get() {
            return jpaConfig;
        }
    }

    /**
     * Create a default JPA configuration with the given name and unit name.
     * @param  name     the name for the enitity manager factory
     * @param  unitName the persistence unit name as used in `persistence.xml`
     * @return          a default JPA configuration
     */
    public static JPAConfig of(String name, String unitName) {
        return new DefaultJPAConfig(new JPAConfig.PersistenceUnit(name, unitName));
    }

    /**
     * Create a default JPA configuration with the given names and unit names.
     */
    public static JPAConfig of(String n1, String u1, String n2, String u2) {
        return new DefaultJPAConfig(
            new JPAConfig.PersistenceUnit(n1, u1),
            new JPAConfig.PersistenceUnit(n2, u2)
        );
    }

    /**
     * Create a default JPA configuration with the given names and unit names.
     */
    public static JPAConfig of(String n1, String u1, String n2, String u2, String n3, String u3) {
        return new DefaultJPAConfig(
            new JPAConfig.PersistenceUnit(n1, u1),
            new JPAConfig.PersistenceUnit(n2, u2),
            new JPAConfig.PersistenceUnit(n3, u3)
        );
    }

    /**
     * Create a default JPA configuration from a map of names to unit names.
     */
    public static JPAConfig from(Map<String, String> map) {
        ImmutableSet.Builder<JPAConfig.PersistenceUnit> persistenceUnits = new ImmutableSet.Builder<JPAConfig.PersistenceUnit>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            persistenceUnits.add(new JPAConfig.PersistenceUnit(entry.getKey(), entry.getValue()));
        }
        return new DefaultJPAConfig(persistenceUnits.build());
    }
}
