/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db.evolutions;

import play.api.db.evolutions.DatabaseEvolutions;
import play.db.Database;

import java.util.*;

/**
 * Utilities for working with evolutions.
 */
public class Evolutions {

    /**
     * Create an evolutions reader that reads evolution files from this class's own classloader.
     *
     * Only useful in simple classloading environments, such as when the classloader structure is flat.
     * @return the evolutions reader.
     */
    public static play.api.db.evolutions.EvolutionsReader fromClassLoader() {
        return fromClassLoader(Evolutions.class.getClassLoader());
    }

    /**
     * Create an evolutions reader that reads evolution files from a classloader.
     *
     * @param classLoader The classloader to read from.
     *
     * @return the evolutions reader.
     */
    public static play.api.db.evolutions.EvolutionsReader fromClassLoader(ClassLoader classLoader) {
        return fromClassLoader(classLoader, "");
    }

    /**
     * Create an evolutions reader that reads evolution files from a classloader.
     *
     * @param classLoader The classloader to read from.
     * @param prefix A prefix that gets added to the resource file names, for example, this could be used to namespace
     *               evolutions in different environments to work with different databases.
     *
     * @return  the evolutions reader.
     */
    public static play.api.db.evolutions.EvolutionsReader fromClassLoader(ClassLoader classLoader, String prefix) {
        return new play.api.db.evolutions.ClassLoaderEvolutionsReader(classLoader, prefix);
    }

    /**
     * Create an evolutions reader based on a simple map of database names to evolutions.
     *
     * @param evolutions The map of database names to evolutions.
     * @return  the evolutions reader.
     */
    public static play.api.db.evolutions.EvolutionsReader fromMap(Map<String, List<Evolution>> evolutions) {
        return new SimpleEvolutionsReader(evolutions);
    }

    /**
     * Create an evolutions reader for the default database from a list of evolutions.
     *
     * @param evolutions The list of evolutions.
     * @return the evolutions reader.
     */
    public static play.api.db.evolutions.EvolutionsReader forDefault(Evolution... evolutions) {
        Map<String, List<Evolution>> map = new HashMap<String, List<Evolution>>();
        map.put("default", Arrays.asList(evolutions));
        return fromMap(map);
    }

    /**
     * Apply evolutions for the given database.
     *
     * @param database The database to apply the evolutions to.
     * @param reader The reader to read the evolutions.
     * @param autocommit Whether autocommit should be used.
     * @param schema The schema where all the play evolution tables are saved in
     */
    public static void applyEvolutions(Database database, play.api.db.evolutions.EvolutionsReader reader, boolean autocommit, String schema) {
        DatabaseEvolutions evolutions = new DatabaseEvolutions(database.asScala(), schema);
        evolutions.evolve(evolutions.scripts(reader), autocommit);
    }

    /**
     * Apply evolutions for the given database.
     *
     * @param database The database to apply the evolutions to.
     * @param reader The reader to read the evolutions.
     * @param schema The schema where all the play evolution tables are saved in
     */
    public static void applyEvolutions(Database database, play.api.db.evolutions.EvolutionsReader reader, String schema) {
        applyEvolutions(database, reader, true, schema);
    }

    /**
     * Apply evolutions for the given database.
     *
     * @param database The database to apply the evolutions to.
     * @param reader The reader to read the evolutions.
     * @param autocommit Whether autocommit should be used.
     */
    public static void applyEvolutions(Database database, play.api.db.evolutions.EvolutionsReader reader, boolean autocommit) {
        applyEvolutions(database, reader, autocommit, "");
    }

    /**
     * Apply evolutions for the given database.
     *
     * @param database The database to apply the evolutions to.
     * @param reader The reader to read the evolutions.
     */
    public static void applyEvolutions(Database database, play.api.db.evolutions.EvolutionsReader reader) {
        applyEvolutions(database, reader, true);
    }

    /**
     * Apply evolutions for the given database.
     *
     * @param database The database to apply the evolutions to.
     * @param schema The schema where all the play evolution tables are saved in
     */
    public static void applyEvolutions(Database database, String schema) {
        applyEvolutions(database, fromClassLoader(), schema);
    }

    /**
     * Apply evolutions for the given database.
     *
     * @param database The database to apply the evolutions to.
     */
    public static void applyEvolutions(Database database) {
        applyEvolutions(database, "");
    }

    /**
     * Cleanup evolutions for the given database.
     *
     * This will run the down scripts for all the applied evolutions.
     *
     * @param database The database to apply the evolutions to.
     * @param autocommit Whether autocommit should be used.
     * @param schema The schema where all the play evolution tables are saved in
     */
    public static void cleanupEvolutions(Database database, boolean autocommit, String schema) {
        DatabaseEvolutions evolutions = new DatabaseEvolutions(database.asScala(), schema);
        evolutions.evolve(evolutions.resetScripts(), autocommit);
    }

    /**
     * Cleanup evolutions for the given database.
     *
     * This will run the down scripts for all the applied evolutions.
     *
     * @param database The database to apply the evolutions to.
     * @param autocommit Whether autocommit should be used.
     */
    public static void cleanupEvolutions(Database database, boolean autocommit) {
        cleanupEvolutions(database, autocommit, "");
    }

    /**
     * Cleanup evolutions for the given database.
     *
     * This will run the down scripts for all the applied evolutions.
     *
     * @param database The database to apply the evolutions to.
     * @param schema The schema where all the play evolution tables are saved in
     */
    public static void cleanupEvolutions(Database database, String schema) {
        cleanupEvolutions(database, true, schema);
    }

    /**
     * Cleanup evolutions for the given database.
     *
     * This will run the down scripts for all the applied evolutions.
     *
     * @param database The database to apply the evolutions to.
     */
    public static void cleanupEvolutions(Database database) {
        cleanupEvolutions(database, "");
    }
}
