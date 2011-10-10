package play;

import java.util.*;
import scala.collection.JavaConverters;

public class Configuration {

    public static Configuration root() {
        return new Configuration(
            play.api.Play.unsafeApplication().configuration()
        );
    }

    // --

    private final play.api.Configuration conf;

    public Configuration(play.api.Configuration conf) {
        this.conf = conf;
    }

    // --

    public Configuration getSub(String key) {
        scala.Option<play.api.Configuration> nConf = conf.getSub(key);
        if(nConf.isDefined()) {
            return new Configuration(nConf.get());
        }
        return null;
    }

    public String getString(String key) {
        return orNull(conf.getString(key, scala.Option.<scala.collection.immutable.Set<java.lang.String>>empty()));
    }

    public Integer getInt(String key) {
        return (Integer)orNull(conf.getInt(key));
    }

    public Boolean getBoolean(String key) {
        return (Boolean)orNull(conf.getBoolean(key));
    }

    public Set<String> keys() {
        return JavaConverters.setAsJavaSetConverter(conf.keys()).asJava();
    }

    public RuntimeException reportError(String key, String message, Throwable e) {
        return conf.reportError(key, message, scala.Option.apply(e));
    }

    // --

    static <T> T orNull(scala.Option<T> opt) {
        if(opt.isDefined()) {
            return opt.get();
        }
        return null;
    }

}