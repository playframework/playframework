package play;

/**
 * High-level API to access Play global features.
 */
public class Play {

    /**
     * Returns the currently running application, or `null` if not defined.
     */
    public static Application application() {
        play.api.Application app = play.api.Play.unsafeApplication();
        if(app == null) {
            return null;
        }
        return new Application(app);
    }

}
