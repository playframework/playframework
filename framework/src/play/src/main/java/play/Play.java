/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

/**
 * High-level API to access Play global features.
 *
 * @deprecated Please use dependency injection.  Deprecated since 2.5.0.
 */
@Deprecated
public class Play {

    /**
     * @deprecated inject the {@link play.Application} instead.   Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static Application application() {
        return privateCurrent().injector().instanceOf(Application.class);
    }

    private static play.api.Application privateCurrent() {
        return play.api.Play.current();
    }

}
