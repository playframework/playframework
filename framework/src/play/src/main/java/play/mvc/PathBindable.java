package play.mvc;

import play.libs.F.*;

/**
 * Binder for URL path parameters.
 */
public interface PathBindable<T> {
    
    /**
     * Bind an URL path parameter.
     *
     * @param key Parameter key
     * @param value The value as String (extracted from the URL path)
     */
    public T bind(String key, String txt);
    
    /**
     * Unbind a URL path  parameter.
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