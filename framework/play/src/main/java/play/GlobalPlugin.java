package play;

import play.api.Application;

/**
 * The GlobalPlugin is the last plugin to start and the first one to stop.
 * It will call your application's onStart and onStop methods, a convenient shortcut for a Bootstrap.
 *
 * @author N.Martignole
 */
public class GlobalPlugin extends Plugin {
    final Application application;

    public GlobalPlugin(Application application) {
        this.application = application;
    }

    /**
     * Executes onStart on the new application GlobalSettings.
     */
    @Override
    public final void onStart() {
        if (application != null) {
            if (application.global() != null) {
                application.global().onStart(application);
            }
        }
    }

    /**
     * Executes onStop on the current application GlobalSettings before we switch to either a new application or null.
     */
    @Override
    public final void onStop() {
        if (application != null) {
            if (application.global() != null) {
                application.global().onStop(application);
            }
        }
    }
}
