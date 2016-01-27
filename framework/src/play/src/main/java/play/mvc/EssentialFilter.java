package play.mvc;

public abstract class EssentialFilter implements play.api.mvc.EssentialFilter {
    public abstract EssentialAction apply(play.mvc.EssentialAction next);

    @Override
    public play.mvc.EssentialAction apply(play.api.mvc.EssentialAction next) {
        return apply(EssentialAction.fromScala(next));
    }
}
