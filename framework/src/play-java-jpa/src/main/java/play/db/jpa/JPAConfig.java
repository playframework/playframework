/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.jpa;

import java.util.Set;

/**
 * JPA configuration.
 */
public interface JPAConfig {

    Set<PersistenceUnit> persistenceUnits();

    class PersistenceUnit {
        public String name;
        public String unitName;
        
        public PersistenceUnit(String name, String unitName) {
            this.name = name;
            this.unitName = unitName;
        }
    }
}
