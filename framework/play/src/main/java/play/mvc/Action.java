package play.mvc;

import play.core.j.*;

import play.mvc.Http.*;

public abstract class Action<T> {

    public T configuration;
    public Action<?> deleguate;
    public abstract Result call(Context ctx);

    public static abstract class Simple extends Action<Void> {}

}