/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import java.util.*;

/**
 * Binder for query string parameters.
 *
 * Any type <code>T</code> that implements this class can be bound to/from query one or more query string parameters.
 * The only requirement is that the class provides a noarg constructor.
 *
 * For example, the following type could be used to encode pagination:
 *
 * <pre>
 * class Pager implements QueryStringBindable&lt;Pager&gt; {
 *     public int index;
 *     public int size;
 *
 *     public Optional&lt;Pager&gt; bind(String key, Map&lt;String, String[]&gt; data) {
 *         if (data.contains(key + ".index" &amp;&amp; data.contains(key + ".size") {
 *             try {
 *                 index = Integer.parseInt(data.get(key + ".index")[0]);
 *                 size = Integer.parseInt(data.get(key + ".size")[0]);
 *                 return Optional.&lt;Pager&gt;ofNullable(this);
 *             } catch (NumberFormatException e) {
 *                 return Optional.&lt;Pager&gt;empty();
 *             }
 *         } else {
 *             return Optional.&lt;Pager&gt;empty();
 *         }
 *     }
 *
 *     public String unbind(String key) {
 *         return key + ".index=" + index + "&amp;" + key + ".size=" + size;
 *     }
 *
 *     public String javascriptUnbind() {
 *         return "function(k,v) {\n" +
 *             "    return encodeURIComponent(k+'.index')+'='+v.index+'&amp;'+encodeURIComponent(k+'.size')+'='+v.size;\n" +
 *             "}";
 *     }
 * }
 * </pre>
 *
 * Then, to match the URL <code>/foo?p.index=5&amp;p.size=42</code>, you could define the following route:
 *
 * <pre>
 * GET  /foo     controllers.Application.foo(p: Pager)
 * </pre>
 *
 * Of course, you could ignore the <code>p</code> key specified in the routes file and just use hard coded index and
 * size parameters if you pleased.
 */
public interface QueryStringBindable<T extends QueryStringBindable<T>> {

    /**
     * Bind a query string parameter.
     *
     * @param key Parameter key
     * @param data The query string data
     * @return An instance of this class (it could be this class) if the query string data can be bound to this type,
     *      or None if it couldn't.
     */
    Optional<T> bind(String key, Map<String,String[]> data);

    /**
     * Unbind a query string parameter.  This should return a query string fragment, in the form
     * <code>key=value[&amp;key2=value2...]</code>.
     *
     * @param key Parameter key
     * @return this key's query-string fragment.
     */
    String unbind(String key);

    /**
     * Javascript function to unbind in the Javascript router.
     *
     * If this bindable just represents a single value, you may return null to let the default implementation handle it.
     *
     * @return null for default behavior, otherwise a valid javascript function that accepts the key and value as
     *         arguments and returns a valid query string fragment (in the format <code>key=value</code>)
     */
    String javascriptUnbind();
}
