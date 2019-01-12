/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import play.i18n.Lang;
import play.i18n.MessagesApi;

import static play.mvc.Http.*;

/**
 * Superclass for a Java-based controller.
 */
public abstract class Controller extends Results implements Status, HeaderNames {

    /**
     * Generates a 501 NOT_IMPLEMENTED simple result.
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #TODO(Request)} instead.
     */
    @Deprecated
    public static Result TODO() {
        return TODO(Http.Context.current().request());
    }

    /**
     * Generates a 501 NOT_IMPLEMENTED simple result.
     */
    public static Result TODO(Request request) {
        return status(NOT_IMPLEMENTED, views.html.defaultpages.todo.render(request.asScala()));
    }

    /**
     * Returns the current HTTP context.
     *
     * @return the context
     *
     * @deprecated Deprecated as of 2.7.0. <a href="https://www.playframework.com/documentation/latest/JavaHttpContextMigration27">See migration guide.</a>.
     */
    @Deprecated
    public static Context ctx() {
        return Http.Context.current();
    }

    /**
     * Returns the current HTTP request.
     *
     * @return the request
     *
     * @deprecated Deprecated as of 2.7.0. <a href="https://www.playframework.com/documentation/latest/JavaHttpContextMigration27">See migration guide.</a>.
     */
    @Deprecated
    public static Request request() {
        return Http.Context.current().request();
    }

    /**
     * Returns the current lang.
     *
     * @return the language
     *
     * @deprecated Deprecated as of 2.7.0. <a href="https://www.playframework.com/documentation/latest/JavaHttpContextMigration27">See migration guide.</a>.
     */
    @Deprecated
    public static Lang lang() {
        return Http.Context.current().lang();
    }

    /**
     * Change durably the lang for the current user
     *
     * @param code New lang code to use (e.g. "fr", "en-US", etc.)
     * @return true if the requested lang was supported by the application, otherwise false.
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link MessagesApi#setLang(Result, Lang)} or {@link Result#withLang(Lang, MessagesApi)}.
     */
    @Deprecated
    public static boolean changeLang(String code) {
        return Http.Context.current().changeLang(code);
    }

    /**
     * Change durably the lang for the current user
     *
     * @param lang New Lang object to use
     * @return true if the requested lang was supported by the application, otherwise false.
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link MessagesApi#setLang(Result, Lang)} or {@link Result#withLang(Lang, MessagesApi)}.
     */
    @Deprecated
    public static boolean changeLang(Lang lang) {
        return Http.Context.current().changeLang(lang);
    }

    /**
     * Clear the lang for the current user.
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link MessagesApi#clearLang(Result)} or {@link Result#withoutLang(MessagesApi)}.
     */
    @Deprecated
    public static void clearLang() {
        Http.Context.current().clearLang();
    }

    /**
     * Returns the current HTTP response.
     *
     * @return the response
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link Result} instead.
     */
    @Deprecated
    public static Response response() {
        return Http.Context.current().response();
    }

    /**
     * Returns the current HTTP session.
     *
     * @return the session
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link Request#session()} and {@link Result} instead.
     */
    @Deprecated
    public static Session session() {
        return Http.Context.current().session();
    }

    /**
     * Puts a new value into the current session.
     *
     * @param key the key to set into the session
     * @param value the value to set for <code>key</code>
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link Result} instead.
     */
    @Deprecated
    public static void session(String key, String value) {
        session().put(key, value);
    }

    /**
     * Returns a value from the session.
     *
     * @param key the session key
     * @return the value for the provided key, or null if there was no value
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link Result} instead.
     */
    @Deprecated
    public static String session(String key) {
        return session().get(key);
    }

    /**
     * Returns the current HTTP flash scope.
     *
     * @return the flash scope
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link Request#flash()} and {@link Result} instead.
     */
    @Deprecated
    public static Flash flash() {
        return Http.Context.current().flash();
    }

    /**
     * Puts a new value into the flash scope.
     *
     * @param key the key to put into the flash scope
     * @param value the value corresponding to <code>key</code>
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link Result} instead.
     */
    @Deprecated
    public static void flash(String key, String value) {
        flash().put(key, value);
    }

    /**
     * Returns a value from the flash scope.
     *
     * @param key the key to look up in the flash scope
     * @return the value corresponding to <code>key</code> from the flash scope, or null if there was none
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link Result} instead.
     */
    @Deprecated
    public static String flash(String key) {
        return flash().get(key);
    }

}
