package play;

import java.util.*;
import scala.collection.JavaConverters;

/**
 * The current application configuration.
 */
public class Configuration {
    
    /**
     * The root configuration.
     *
     * @return A Configuration instance.
     */
    public static Configuration root() {
        return new Configuration(
            play.api.Play.unsafeApplication().configuration()
        );
    }
    
    // --
    
    private final play.api.Configuration conf;

    /**
     * Create a new configuration from a Scala based configuration.
     */
    public Configuration(play.api.Configuration conf) {
        this.conf = conf;
    }
    
    // --
    
    /**
     * Retrieve a sub configuration, ie. a configuration instance containing all key starting with a prefix.
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
     * Retrieve a configuration value as String.
     *
     * @param key Configuration key (relative to configuration root key).
     * @return Maybe a configuration value or null.
     */
    public String getString(String key) {
        return orNull(conf.getString(key, scala.Option.<scala.collection.immutable.Set<java.lang.String>>empty()));
    }
    
    /**
     * Retrieve a configuration value as Int.
     *
     * @param key Configuration key (relative to configuration root key).
     * @return Maybe a configuration value or null.
     */
    public Integer getInt(String key) {
        return (Integer)orNull(conf.getInt(key));
    }
    
    /**
     * Retrieve a configuration value as Boolean.
     *
     * @param key Configuration key (relative to configuration root key).
     * @return Maybe a configuration value or null.
     */
    public Boolean getBoolean(String key) {
        return (Boolean)orNull(conf.getBoolean(key));
    }
    
    /**
     * Retrieve the set of keys available in this configuration.
     *
     * @return The set of keys available in this configuration.
     */
    public Set<String> keys() {
        return JavaConverters.setAsJavaSetConverter(conf.keys()).asJava();
    }
    
    /**
     * Create a configuration error for a specific congiguration key.
     *
     * @param key The configuration key, related to this error.
     * @param message The error message.
     * @param e Maybe the related exception.
     * @return A configuration exception.
     */
    public RuntimeException reportError(String key, String message, Throwable e) {
        return conf.reportError(key, message, scala.Option.apply(e));
    }
    
    // -- Utils
    
    private static <T> T orNull(scala.Option<T> opt) {
        if(opt.isDefined()) {
            return opt.get();
        }
        return null;
    }
    
}