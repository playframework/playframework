package play;

import play.mvc.Result;

/**
 * GlobalSettings is instanciated by the framework when an application starts, to let you perform specific tasks
 * at startup or shutdown.
 * How to use it : create a Global.java class file in your java application and override the methods you need
 * to use.
 * @author N.Martignole
 */
public abstract class GlobalSettings {
    /**
     * Executed before any plugin, you can setup your database schema here for instance
     */
    public void beforeStart(Application app) {
    }

    /**
     * Executed after all plugins, including the database setup with Evolution and the EBean wrapper.
     * This is a good place to execute some of your application code to create entries for instance.
     */
    public void onStart(Application app) {
    }

    /**
     * Executed after all plugins, including the database setup with Evolution and the EBean wrapper.
     */
    public void onStop(Application app) {
    }

    /**
     * Returns a Result that could be a custom error page.
     * The default implementation returns null, so that the scala engine handles the excepetion and show an error page.
     * @param t is any throwable
     * @return null as the default impl
     */
    public Result onError(Throwable t) {
        return null;
    }

    /**
     * Triggered when a resource was request but not found, the default implementation returns null, so that
     * the scala engine handles the onActionNotFound.
     * @param uri
     * @return null in the default implementation, you can return your own custom Result in your Global class.
     */
    public Result onActionNotFound(String uri) {
        return null;
    }
}
