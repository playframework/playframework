/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.util.List;
import javax.sql.DataSource;

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
