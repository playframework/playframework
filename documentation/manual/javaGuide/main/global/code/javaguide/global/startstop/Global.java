package javaguide.global.startstop;

//#global
import play.*;

public class Global extends GlobalSettings {

    public void onStart(Application app) {
        Logger.info("Application has started");
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }

}
//#global
