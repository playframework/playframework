//###replace: package tasks;
package javaguide.scheduling;

import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.NoHttpFiltersComponents;
import play.routing.Router;

public class MyBuiltInComponentsFromContext
        extends BuiltInComponentsFromContext
        implements NoHttpFiltersComponents {

    // Task is initialize here
    private final CodeBlockTask task = new CodeBlockTask(actorSystem(), executionContext());

    public MyBuiltInComponentsFromContext(ApplicationLoader.Context context) {
        super(context);
    }

    @Override
    public Router router() {
        return Router.empty();
    }
}
