/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.evolutions;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** A simple evolutions reader that uses a map to store evolutions */
public class SimpleEvolutionsReader extends EvolutionsReader {
  private final Map<String, List<Evolution>> evolutions;

  public SimpleEvolutionsReader(Map<String, List<Evolution>> evolutions) {
    this.evolutions = evolutions;
  }

  @Override
  public Collection<Evolution> getEvolutions(String db) {
    return evolutions.get(db);
  }
}
