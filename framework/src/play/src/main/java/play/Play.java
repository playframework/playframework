package play;

public class Play {

    public static Application application() {
        play.api.Application app = play.api.Play.unsafeApplication();
        if(app == null) {
            return null;
        }
        return new Application(app);
    }

}
