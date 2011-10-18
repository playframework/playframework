import play.Application;
import play.GlobalSettings;
import play.mvc.Result;

/**
 * Executes some database setup when the application starts.
 * The class name must be Global.
 */
public final class Global extends GlobalSettings {
    /**
     * Executed before any plugin is loaded. This is a good place to perform system operation.
     *
     * @param app is the current application
     */
    @Override
    public void beforeStart(Application app) {
        System.out.println("beforeStart called");
    }

    /**
     * Executed once all plugins have been started : the EBean and the Database should be up and ready
     * to accept some operation.
     *
     * @param app is the current application
     */
    @Override
    public void onStart(Application app) {
        System.out.println("onStart...");

        // Create some test entries
        // todo
    }

    /**
     * Executed before shutting down the application
     *
     * @param app is the current application
     */
    @Override
    public void onStop(Application app) {
        System.out.println("onStop...");
    }

    /**
     * Triggered by the framework upon error. Here you can return a custom result for a specific throwable.
     * If you return null, Play will display its custom error page in DEV mode or a server error in Prod mode.
     *
     * @param t is any throwable
     * @return my custom result or null.
     */
    @Override
    public Result onError(Throwable t) {
        System.out.println("onError with " + t);
        return new Result.InternalServerError(t.getMessage());
    }

    /**
     * Triggered when an action was not found.
     *
     * @param uri is the request
     * @return either a custom Result or null.
     */
    @Override
    public Result onActionNotFound(String uri) {
        if (uri != null) {
            return new Result.NotFound("Custom not found for URI=" + uri);
        } else {
            return super.onActionNotFound(uri);
        }
    }
}
