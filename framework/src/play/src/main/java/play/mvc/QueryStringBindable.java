package play.mvc;

import java.util.*;

import play.libs.F.*;

/**
 * Binder for query string parameters.
 */
public interface QueryStringBindable<T> {
    
    /**
     * Bind a query string parameter.
     *
     * @param key Parameter key
     * @param params QueryString data
     */
    public Option<T> bind(String key, Map<String,String[]> data);
    
    /**
     * Unbind a query string parameter.
     *
     * @param key Parameter key
     * @param value Parameter value.
     */
    public String unbind(String key);
    
    /**
     * Javascript function to unbind in the Javascript router.
     */
    public String javascriptUnbind();
    
}