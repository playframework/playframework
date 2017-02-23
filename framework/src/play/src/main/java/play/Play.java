/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import play.core.j.JavaModeConverter$;

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

    /**
     * @deprecated inject the {@link play.Environment} instead.   Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static Mode mode() {
        return JavaModeConverter$.MODULE$.asJavaMode(play.api.Play.mode(privateCurrent()));
    }

    /**
     * @deprecated inject the {@link play.Environment} instead.   Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static boolean isDev() {
        return play.api.Play.isDev(privateCurrent());
    }

    /**
     * @deprecated inject the {@link play.Environment} instead.   Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static boolean isProd() {
        return play.api.Play.isProd(privateCurrent());
    }

    /**
     * @deprecated inject the {@link play.Environment} instead.   Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static boolean isTest() {
        return play.api.Play.isTest(privateCurrent());
    }

    /**
     * @deprecated Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static String langCookieName() {
        return play.api.i18n.Messages.Implicits$.MODULE$.applicationMessagesApi(privateCurrent()).langCookieName();
    }

    /**
     * @deprecated Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static boolean langCookieSecure() {
        return play.api.i18n.Messages.Implicits$.MODULE$.applicationMessagesApi(privateCurrent()).langCookieSecure();
    }

    /**
     * @deprecated Deprecated since 2.5.0.
     * @return Deprecated
     */
    @Deprecated
    public static boolean langCookieHttpOnly() {
        return play.api.i18n.Messages.Implicits$.MODULE$.applicationMessagesApi(privateCurrent()).langCookieHttpOnly();
    }

    private static play.api.Application privateCurrent() {
        return play.api.Play.current();
    }

}
