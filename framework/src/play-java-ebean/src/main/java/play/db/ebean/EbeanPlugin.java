package play.db.ebean;

import play.*;
import play.db.*;

import play.api.libs.Files;

import java.io.*;
import java.util.*;

import com.avaje.ebean.*;
import com.avaje.ebean.config.*;
import com.avaje.ebeaninternal.server.ddl.*;
import com.avaje.ebeaninternal.api.*;

/**
 * A Play plugin that automatically manages Ebean configuration.
 */
public class EbeanPlugin extends Plugin {
    
    private final Application application;
    
    public EbeanPlugin(Application application) {
        this.application = application;
    }
    
    // --
    
    private final Map<String,EbeanServer> servers = new HashMap<String,EbeanServer>();
    
    /**
     * Reads the configuration file and initialises required Ebean servers.
     */
    public void onStart() {

        Configuration ebeanConf = Configuration.root().getConfig("ebean");
        
        if(ebeanConf != null) {
            for(String key: ebeanConf.keys()) {
                
                ServerConfig config = new ServerConfig();
                config.setName(key);
                config.loadFromProperties();
                try {
                    config.setDataSource(new WrappingDatasource(DB.getDataSource(key)));
                } catch(Exception e) {
                    throw ebeanConf.reportError(
                        key,
                        e.getMessage(),
                        e
                    );
                }
                if(key.equals("default")) {
                    config.setDefaultServer(true);
                }
                
                String[] toLoad = ebeanConf.getString(key).split(",");
                Set<String> classes = new HashSet<String>();
                for(String load: toLoad) {
                    load = load.trim();
                    if(load.endsWith(".*")) {
                        classes.addAll(play.libs.Classpath.getTypes(application, load.substring(0, load.length()-2)));
                    } else {
                        classes.add(load);
                    }
                }
                for(String clazz: classes) {
                    try {
                        config.addClass(Class.forName(clazz, true, application.classloader()));
                    } catch(Throwable e) {
                        throw ebeanConf.reportError(
                            key,
                            "Cannot register class [" + clazz + "] in Ebean server",
                            e
                        );
                    }
                }
                
                servers.put(key, EbeanServerFactory.create(config));
                
                // DDL
                if(!application.isProd()) {
                    boolean evolutionsEnabled = !"disabled".equals(application.configuration().getString("evolutionplugin"));
                    if(evolutionsEnabled) {
                        String evolutionScript = generateEvolutionScript(servers.get(key), config);
                        if(evolutionScript != null) {
                            File evolutions = application.getFile("conf/evolutions/" + key + "/1.sql");
                            if(!evolutions.exists() || Files.readFile(evolutions).startsWith("# --- Created by Ebean DDL")) {
                                Files.createDirectory(application.getFile("conf/evolutions/" + key));
                                Files.writeFileIfChanged(evolutions, evolutionScript);
                            }
                        }
                    }
                }
                
            }
        }
        
    }
    
    /**
     * Helper method that generates the required evolution to properly run Ebean.
     */
    public static String generateEvolutionScript(EbeanServer server, ServerConfig config) {
        DdlGenerator ddl = new DdlGenerator();
        ddl.setup((SpiEbeanServer)server, config.getDatabasePlatform(), config);
        String ups = ddl.generateCreateDdl();
        String downs = ddl.generateDropDdl();
        
        if(ups == null || ups.trim().isEmpty()) {
            return null;
        }
        
        return (
            "# --- Created by Ebean DDL\r\n" +
            "# To stop Ebean DDL generation, remove this comment and start using Evolutions\r\n" +
            "\r\n" + 
            "# --- !Ups\r\n" +
            "\r\n" + 
            ups +
            "\r\n" + 
            "# --- !Downs\r\n" +
            "\r\n" +
            downs
        );
    }
    
    /**
     * <code>DataSource</code> wrapper to ensure that every retrieved connection has auto-commit disabled.
     */
    static class WrappingDatasource implements javax.sql.DataSource {
        
        public java.sql.Connection wrap(java.sql.Connection connection) throws java.sql.SQLException {
            connection.setAutoCommit(false);
            return connection;
        }
        
        // --
        
        final javax.sql.DataSource wrapped;
        
        public WrappingDatasource(javax.sql.DataSource wrapped) {
            this.wrapped = wrapped;
        }
        
        public java.sql.Connection getConnection() throws java.sql.SQLException {
            return wrap(wrapped.getConnection());
        }
        
        public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
            return wrap(wrapped.getConnection(username, password));
        }
        
        public int getLoginTimeout() throws java.sql.SQLException {
            return wrapped.getLoginTimeout();
        }
        
        public java.io.PrintWriter getLogWriter() throws java.sql.SQLException {
            return wrapped.getLogWriter();
        }
        
        public void setLoginTimeout(int seconds) throws java.sql.SQLException {
            wrapped.setLoginTimeout(seconds);
        }
        
        public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {
            wrapped.setLogWriter(out);
        }
        
        public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
            return wrapped.isWrapperFor(iface);
        }
        
        public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
            return wrapped.unwrap(iface);
        }

        public java.util.logging.Logger getParentLogger() {
            return null;
        }
        
    }
    
    
}
