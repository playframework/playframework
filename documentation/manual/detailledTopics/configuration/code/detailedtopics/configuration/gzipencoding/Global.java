package detailedtopics.configuration.gzipencoding;

//#global
import play.GlobalSettings;
import play.api.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;

public class Global extends GlobalSettings {
    public <T extends EssentialFilter> Class<T>[] filters() {
        return new Class[]{GzipFilter.class};
    }
}
//#global
