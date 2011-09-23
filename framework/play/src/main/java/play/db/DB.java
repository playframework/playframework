package play.db;

import java.sql.*;
import javax.sql.*;

public class DB {
    
    public static DataSource getDataSource() {
        return getDataSource("default");
    }
    
    public static DataSource getDataSource(String database) {
        return play.api.db.DB.getDataSource(database, play.api.Play.unsafeApplication());
    }
    
    public static Connection getConnection() {
        return getConnection("default");
    }
    
    public static Connection getConnection(boolean autocommit) {
        return getConnection("default", autocommit);
    }
    
    public static Connection getConnection(String database) {
        return getConnection(database, true);
    }
    
    public static Connection getConnection(String database, boolean autocommit) {
        return play.api.db.DB.getConnection(database, autocommit, play.api.Play.unsafeApplication());
    }
    
}