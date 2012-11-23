package play;

/**
 * High-level API to access Play global features.
 */
public class Play {

    /**
     * Returns the currently running application.
     */
    public static Application application() {
        return new Application(play.api.Play.current());
    }
    
    /**
     * Returns `true` if the current application is `DEV` mode.
     */
    public static boolean isDev() {
        return play.api.Play.isDev(play.api.Play.current());
    }
    
    /**
     * Returns `true` if the current application is `PROD` mode.
     */
    public static boolean isProd() {
        return play.api.Play.isProd(play.api.Play.current());
    }
    
    /**
     * Returns `true` if the current application is `TEST` mode.
     */
    public static boolean isTest() {
        return play.api.Play.isTest(play.api.Play.current());
    }

    public static String langCookieName() {
        return play.api.Play.langCookieName(play.api.Play.current());
    }
}
