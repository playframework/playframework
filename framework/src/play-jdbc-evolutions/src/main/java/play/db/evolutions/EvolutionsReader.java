/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.evolutions;

import play.libs.Scala;
import scala.collection.Seq;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Reads evolutions.
 */
public abstract class EvolutionsReader implements play.api.db.evolutions.EvolutionsReader {
    public final Seq<play.api.db.evolutions.Evolution> evolutions(String db) {
        Collection<Evolution> evolutions = getEvolutions(db);
        if (evolutions != null) {
            List<play.api.db.evolutions.Evolution> scalaEvolutions = evolutions.stream()
                    .map(e -> new play.api.db.evolutions.Evolution(e.getRevision(), e.getSqlUp(), e.getSqlDown()))
                    .collect(toList());
            return Scala.asScala(scalaEvolutions);
        } else {
            return Scala.asScala(Collections.emptyList());
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
