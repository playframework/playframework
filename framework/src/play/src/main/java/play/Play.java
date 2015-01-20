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
     * Returns the currently running application.
     */
    public static Application application() {
        return play.api.Play.current().injector().instanceOf(Application.class);
    }

    /**
     * Returns the current mode of the application.
     */
    public static Mode mode() {
        return JavaModeConverter$.MODULE$.asJavaMode(play.api.Play.mode(play.api.Play.current()));
    }

    /**
     * Returns `true` if the current application is `DEV` mode.
     */
    public static boolean isDev() {
        return play.api.Play.isDev(play.api.Play.current());
    }

    /**
     * Returns `true` if the current application is `PROD` mode.
     */
    public static boolean isProd() {
        return play.api.Play.isProd(play.api.Play.current());
    }

    /**
     * Returns `true` if the current application is `TEST` mode.
     */
    public static boolean isTest() {
        return play.api.Play.isTest(play.api.Play.current());
    }

    public static String langCookieName() {
        return play.api.i18n.Messages.Implicits$.MODULE$.applicationMessagesApi(play.api.Play.current()).langCookieName();
    }
}
