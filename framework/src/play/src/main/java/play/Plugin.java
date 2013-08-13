package play;

/**
 * A Play plugin.
 *
 * A plugin must define a single argument constructor that accepts an {@link play.Application}, for example:
 *
 * <pre>
 * public class MyPlugin extends Plugin {
 *     private final Application app;
 *     public MyPlugin(Application app) {
 *         this.app = app;
 *     }
 *     public void onStart() {
 *         Logger.info("Plugin started!");
 *     }
 * }
 * </pre>
 */
public class Plugin implements play.api.Plugin {
    
    /**
     * Called when the application starts.
     */
    public void onStart() {
    }
    
    /**
     * Called when the application stops.
     */
    public void onStop() {
    }
    
    /**
     * Is this plugin enabled.
     */ 
    public boolean enabled() { 
        return true;
    }

}
