package play.db;

import com.typesafe.config.Config;
import play.Environment;

public interface AsyncConnectionPool {

    /**
     * Create a data source with the given configuration.
     *
     * @param name the database name
     * @param configuration the data source configuration
     * @param environment the database environment
     * @return a data source backed by a connection pool
     */
    AsyncDataSource createAsync(String name, Config configuration, Environment environment);

    play.api.db.AsyncConnectionPool asScala();

}
