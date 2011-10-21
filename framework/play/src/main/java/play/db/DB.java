package play.db;

import java.sql.*;
import javax.sql.*;

/**
 * Provides high level API to get JDBC connections.
 */
public class DB {
    
    /**
     * Get the default datasource.
     */
    public static DataSource getDataSource() {
        return getDataSource("default");
    }
    
    /**
     * Get any default datasource.
     */
    public static DataSource getDataSource(String database) {
        return play.api.db.DB.getDataSource(database, play.api.Play.unsafeApplication());
    }
    
    /**
     * Get a connection from the default datasource (autocommit is set to true).
     */
    public static Connection getConnection() {
        return getConnection("default");
    }
    
    /**
     * Get a connection from the default datasource with custom autocommit setting.
     */
    public static Connection getConnection(boolean autocommit) {
        return getConnection("default", autocommit);
    }
    
    /**
     * Get a connection from any datasource (autocommit is set to true).
     */
    public static Connection getConnection(String database) {
        return getConnection(database, true);
    }
    
    /**
     * Get a connection from any datasource with custom autocommit setting.
     */
    public static Connection getConnection(String database, boolean autocommit) {
        return play.api.db.DB.getConnection(database, autocommit, play.api.Play.unsafeApplication());
    }
    
}