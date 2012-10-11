package play.db;

import java.sql.*;
import javax.sql.*;

/**
 * Provides a high-level API for getting JDBC connections.
 */
public class DB {
    
    /**
     * Returns the default datasource.
     */
    public static DataSource getDataSource() {
        return getDataSource("default");
    }
    
    /**
     * Returns any default datasource.
     */
    public static DataSource getDataSource(String database) {
        return play.api.db.DB.getDataSource(database, play.api.Play.unsafeApplication());
    }
    
    /**
     * Returns a connection from the default datasource, with auto-commit enabled.
     */
    public static Connection getConnection() {
        return getConnection("default");
    }
    
    /**
     * Returns a connection from the default datasource, with the specified auto-commit setting.
     */
    public static Connection getConnection(boolean autocommit) {
        return getConnection("default", autocommit);
    }
    
    /**
     * Returns a connection from any datasource, with auto-commit enabled.
     */
    public static Connection getConnection(String database) {
        return getConnection(database, true);
    }
    
    /**
     * Get a connection from any datasource, with the specified auto-commit setting.
     */
    public static Connection getConnection(String database, boolean autocommit) {
        return play.api.db.DB.getConnection(database, autocommit, play.api.Play.unsafeApplication());
    }
    
}