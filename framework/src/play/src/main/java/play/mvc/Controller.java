package play.mvc;

import play.i18n.Lang;
import play.mvc.Http.*;


/**
 * Superclass for a Java-based controller.
 */
public abstract class Controller extends Results implements Status, HeaderNames {

    /**
     * Returns the current HTTP context.
     */
    public static Context ctx() {
        return Http.Context.current();
    }

    /**
     * Returns the current HTTP request.
     */
    public static Request request() {
        return Http.Context.current().request();
    }

    /**
     * Returns the current lang.
     */
    public static Lang lang() {
        return Http.Context.current().lang();
    }

    /**
     * Change durably the lang for the current user
     * @param code New lang code to use (e.g. "fr", "en_US", etc.)
     * @return true if the requested lang was supported by the application, otherwise false.
     */
    public static boolean changeLang(String code) {
        return Http.Context.current().changeLang(code);
    }

    /**
     * Clear the lang for the current user.
     */
    public static void clearLang() {
        Http.Context.current().clearLang();
    }

    /**
     * Returns the current HTTP response.
     */
    public static Response response() {
        return Http.Context.current().response();
    }

    /**
     * Returns the current HTTP session.
     */
    public static Session session() {
        return Http.Context.current().session();
    }

    /**
     * Puts a new value into the current session.
     */
    public static void session(String key, String value) {
        session().put(key, value);
    }

    /**
     * Returns a value from the session.
     */
    public static String session(String key) {
        return session().get(key);
    }

    /**
     * Returns the current HTTP flash scope.
     */
    public static Flash flash() {
        return Http.Context.current().flash();
    }

    /**
     * Puts a new value into the flash scope.
     */
    public static void flash(String key, String value) {
        flash().put(key, value);
    }

    /**
     * Returns a value from the flash scope.
     */
    public static String flash(String key) {
        return flash().get(key);
    }

}
