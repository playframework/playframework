package play.mvc;

import play.core.j.*;

import play.mvc.Http.*;

/**
 * An action acts as decorator for the action method call.
 */
public abstract class Action<T> extends Results {
    
    /**
     * The action configuration (typically the annotation used to decorate the action method).
     */
    public T configuration;
    
    /**
     * The wrapped action.
     */
    public Action<?> deleguate;
    
    /**
     * Execute this action with the give HTTP context and return the result.
     */
    public abstract Result call(Context ctx);
    
    /**
     * A simple action with no configuration.
     */
    public static abstract class Simple extends Action<Void> {}
    
}