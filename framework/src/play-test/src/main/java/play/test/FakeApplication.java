package play.test;

import java.io.*;
import java.util.*;

import play.api.mvc.Handler;
import play.libs.*;
import scala.PartialFunction$;
import scala.Tuple2;

/**
 * A Fake application.
 */
public class FakeApplication {

    final play.api.test.FakeApplication wrappedApplication;

    /**
     * A Fake application.
     *
     * @param path The application path
     * @param classloader The application classloader
     * @param additionalConfiguration Additional configuration
     * @param additionalPlugins Additional plugins
     * @param withoutPlugins Plugins to disable
     */
    @SuppressWarnings("unchecked")
    public FakeApplication(File path, ClassLoader classloader, Map<String, ? extends Object> additionalConfiguration,
            List<String> additionalPlugins, List<String> withoutPlugins, play.GlobalSettings global) {
        play.api.GlobalSettings g = null;
        if(global != null)
          g = new play.core.j.JavaGlobalSettingsAdapter(global);
        wrappedApplication = new play.api.test.FakeApplication(
                path,
                classloader,
                Scala.toSeq(additionalPlugins),
                Scala.toSeq(withoutPlugins),
                Scala.asScala((Map<String, Object>)additionalConfiguration),
                scala.Option.apply(g),
                PartialFunction$.MODULE$.<Tuple2<String, String>, Handler>empty()
                );
    }

    public FakeApplication(File path, ClassLoader classloader, Map<String, ? extends Object> additionalConfiguration,
                           List<String> additionalPlugins, play.GlobalSettings global) {
        this(path, classloader, additionalConfiguration, additionalPlugins, Collections.<String>emptyList(), global);
    }

    public play.api.test.FakeApplication getWrappedApplication() {
        return wrappedApplication;
    }

}
