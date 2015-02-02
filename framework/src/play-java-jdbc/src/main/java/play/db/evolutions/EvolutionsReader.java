/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db.evolutions;

import play.libs.Scala;
import scala.collection.Seq;

import java.util.*;

/**
 * Reads evolutions.
 */
public abstract class EvolutionsReader implements play.api.db.evolutions.EvolutionsReader {
    public final Seq<play.api.db.evolutions.Evolution> evolutions(String db) {
        Collection<Evolution> evolutions = getEvolutions(db);
        if (evolutions != null) {
            List<play.api.db.evolutions.Evolution> scalaEvolutions = new ArrayList<play.api.db.evolutions.Evolution>(evolutions.size());
            for (Evolution e: evolutions) {
                scalaEvolutions.add(new play.api.db.evolutions.Evolution(e.getRevision(), e.getSqlUp(), e.getSqlDown()));
            }
            return Scala.asScala(scalaEvolutions);
        } else {
            return Scala.asScala(Collections.<play.api.db.evolutions.Evolution>emptyList());
        }
    }

    /**
     * Get the evolutions for the given database name.
     *
     * @param db The name of the database.
     * @return The collection of evolutions.
     */
    public abstract Collection<Evolution> getEvolutions(String db);
}
