/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db;

import java.util.List;

/**
 * DB API for managing application databases.
 */
public interface DBApi {

    /**
     * All configured databases.
     */
    public List<Database> getDatabases();

    /**
     * Get database with given configuration name.
     *
     * @param name the configuration name of the database
     */
    public Database getDatabase(String name);

    /**
     * Shutdown all databases, releasing resources.
     */
    public void shutdown();

}
