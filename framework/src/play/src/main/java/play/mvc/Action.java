package play.mvc;

import play.mvc.Http.*;
import static play.libs.F.Promise;

/**
 * An action acts as decorator for the action method call.
 */
public abstract class Action<T> extends Results {
    
    /**
     * The action configuration - typically the annotation used to decorate the action method.
     */
    public T configuration;
    
    /**
     * The wrapped action.
     */
    public Action<?> delegate;
    
    /**
     * Executes this action with the give HTTP context and returns the result.
     */
    public abstract Promise<SimpleResult> call(Context ctx) throws Throwable;
    
    /**
     * A simple action with no configuration.
     */
    public static abstract class Simple extends Action<Void> {}
    
}
