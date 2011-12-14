package play;

import java.util.*;
import scala.collection.JavaConverters;

import play.libs.Scala;
import com.typesafe.config.Config;

/**
 * The current application configuration.
 */
public class Configuration {
    
    /**
     * The root configuration.
     * <p>
     * @return a Configuration instance
     */
    public static Configuration root() {
        return new Configuration(
            play.api.Play.unsafeApplication().configuration()
        );
    }
    
    // --
    
    private final play.api.Configuration conf;

    /**
     * Creates a new configuration from a Scala-based configuration.
     */
    public Configuration(play.api.Configuration conf) {
        this.conf = conf;
    }
    
    // --
    
    /**
     * loads application.conf, application.properties, application.json resource from classpath
     * @param resource to load
     * @return config 
     */
    public Config load(String resource) {
        return conf.loadAsJava(resource);
    }

    /**
     * Retrieves a sub-configuration, which is a configuration instance containing all keys that start with the given prefix.
     *
     * @param key The root prefix for this sub configuration.
     * @return Maybe a new configuration
     */
    public Configuration getSub(String key) {
        scala.Option<play.api.Configuration> nConf = conf.getSub(key);
        if(nConf.isDefined()) {
            return new Configuration(nConf.get());
        }
        return null;
    }
    
    /**
     * Retrieves a configuration value as a <code>String</code>.
     *
     * @param key configuration key (relative to configuration root key)
     * @return a configuration value or <code>null</code>
     */
    public String getString(String key) {
        return Scala.orNull(conf.getString(key, scala.Option.<scala.collection.immutable.Set<java.lang.String>>empty()));
    }
    
    /**
     * Retrieves a configuration value as an <code>Int</code>.
     *
     * @param key configuration key (relative to configuration root key)
     * @return a configuration value or <code>null</code>
     */
    public Integer getInt(String key) {
        return (Integer)Scala.orNull(conf.getInt(key));
    }
    
    /**
     * Retrieves a configuration value as a <code>Boolean</code>.
     *
     * @param key configuration key (relative to configuration root key)
     * @return a configuration value or <code>null</code>
     */
    public Boolean getBoolean(String key) {
        return (Boolean)Scala.orNull(conf.getBoolean(key));
    }
    
    /**
     * Retrieves the set of keys available in this configuration.
     *
     * @return the set of keys available in this configuration
     */
    public Set<String> keys() {
        return JavaConverters.setAsJavaSetConverter(conf.keys()).asJava();
    }
    
    /**
     * Creates a configuration error for a specific congiguration key.
     *
     * @param key the configuration key, related to this error
     * @param message the error message
     * @param e the optional related exception
     * @return a configuration exception
     */
    public RuntimeException reportError(String key, String message, Throwable e) {
        return conf.reportError(key, message, scala.Option.apply(e));
    }
    
}
