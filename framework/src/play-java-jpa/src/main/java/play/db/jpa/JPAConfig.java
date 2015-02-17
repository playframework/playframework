/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.jpa;

import java.util.Set;

/**
 * JPA configuration.
 */
public interface JPAConfig {

    public Set<PersistenceUnit> persistenceUnits();

    public static class PersistenceUnit {
        public String name;
        public String unitName;
        
        public PersistenceUnit(String name, String unitName) {
            this.name = name;
            this.unitName = unitName;
        }
    }

}
