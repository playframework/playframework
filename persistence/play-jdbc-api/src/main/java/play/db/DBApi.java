/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import java.util.List;

/** DB API for managing application databases. */
public interface DBApi {

  /**
   * @return all configured databases.
   */
  List<Database> getDatabases();

  /**
   * @param name the configuration name of the database
   * @return Get database with given configuration name.
   */
  Database getDatabase(String name);

  /** Shutdown all databases, releasing resources. */
  void shutdown();
}
