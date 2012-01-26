package play;

/**
 * A Play plugin.
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
