package play.mvc;

/**
 * Binder for path parameters.
 *
 * Any type <code>T</code> that implements this class can be bound to/from a path parameter.  The only requirement is
 * that the class provides a noarg constructor.
 *
 * For example, the following type could be used to bind an Ebean user:
 *
 * <pre>
 * @Entity
 * class User extends Model implements PathBindable&lt;User&gt; {
 *     public String email;
 *     public String name;
 *
 *     public User bind(String key, String email) {
 *         User user = findByEmail(email);
 *         if (user != null) {
 *             user;
 *         } else {
 *             throw new IllegalArgumentException("User with email " + email + " not found");
 *         }
 *     }
 *
 *     public String unbind(String key) {
 *         return email;
 *     }
 *
 *     public String javascriptUnbind() {
 *         return "function(k,v) {\n" +
 *             "    return v.email;" +
 *             "}";
 *     }
 *
 *     // Other ebean methods here
 * }
 * </pre>
 *
 * Then, to match the URL <code>/user/bob@example.com</code>, you could define the following route:
 *
 * <pre>
 * GET  /user/:user     controllers.Users.show(user: User)
 * </pre>
 */
public interface PathBindable<T extends PathBindable<T>> {

    /**
     * Bind an URL path parameter.
     *
     * @param key Parameter key
     * @param txt The value as String (extracted from the URL path)
     * @return The object, may be this object
     * @throws RuntimeException if this object could not be bound
     */
    public T bind(String key, String txt);

    /**
     * Unbind a URL path parameter.
     *
     * @param key Parameter key
     */
    public String unbind(String key);

    /**
     * Javascript function to unbind in the Javascript router.
     *
     * @return The javascript function, or null if you want to use the default implementation.
     */
    public String javascriptUnbind();

}