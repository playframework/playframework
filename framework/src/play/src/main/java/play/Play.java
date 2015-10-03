/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play;

import play.core.j.JavaModeConverter$;

/**
 * High-level API to access Play global features.
 */
public class Play {

    /**
     * @deprecated inject the {@link play.Application} instead
     * @return Deprecated
     */
    @Deprecated
    public static Application application() {
        return play.api.Play.current().injector().instanceOf(Application.class);
    }

    /**
     * @deprecated inject the {@link play.Environment} instead
     * @return Deprecated
     */
    @Deprecated
    public static Mode mode() {
        return JavaModeConverter$.MODULE$.asJavaMode(play.api.Play.mode(play.api.Play.current()));
    }

    /**
     * @deprecated inject the {@link play.Environment} instead
     * @return Deprecated
     */
    @Deprecated
    public static boolean isDev() {
        return play.api.Play.isDev(play.api.Play.current());
    }

    /**
     * @deprecated inject the {@link play.Environment} instead
     * @return Deprecated
     */
    @Deprecated
    public static boolean isProd() {
        return play.api.Play.isProd(play.api.Play.current());
    }

    /**
     * @deprecated inject the {@link play.Environment} instead
     * @return Deprecated
     */
    @Deprecated
    public static boolean isTest() {
        return play.api.Play.isTest(play.api.Play.current());
    }

    public static String langCookieName() {
        return play.api.i18n.Messages.Implicits$.MODULE$.applicationMessagesApi(play.api.Play.current()).langCookieName();
    }

    public static boolean langCookieSecure() {
        return play.api.i18n.Messages.Implicits$.MODULE$.applicationMessagesApi(play.api.Play.current()).langCookieSecure();
    }

    public static boolean langCookieHttpOnly() {
        return play.api.i18n.Messages.Implicits$.MODULE$.applicationMessagesApi(play.api.Play.current()).langCookieHttpOnly();
    }
}
