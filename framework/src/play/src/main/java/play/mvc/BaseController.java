/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import play.i18n.Lang;

/**
 * A base controller for Java controllers.
 */
public class BaseController extends Results implements Http.Status, Http.HeaderNames {

    /**
     * Returns the current HTTP context.
     *
     * @return the context
     */
    public Http.Context ctx() {
        return Http.Context.current();
    }

    /**
     * Returns the current HTTP request.
     *
     * @return the request
     */
    public Http.Request request() {
        return Http.Context.current().request();
    }

    /**
     * Returns the current lang.
     *
     * @return the language
     */
    public Lang lang() {
        return Http.Context.current().lang();
    }

    /**
     * Change durably the lang for the current user
     *
     * @param code New lang code to use (e.g. "fr", "en-US", etc.)
     * @return true if the requested lang was supported by the application, otherwise false.
     */
    public boolean changeLang(String code) {
        return Http.Context.current().changeLang(code);
    }

    /**
     * Change durably the lang for the current user
     *
     * @param lang New Lang object to use
     * @return true if the requested lang was supported by the application, otherwise false.
     */
    public boolean changeLang(Lang lang) {
        return Http.Context.current().changeLang(lang);
    }

    /**
     * Clear the lang for the current user.
     */
    public void clearLang() {
        Http.Context.current().clearLang();
    }

    /**
     * Returns the current HTTP response.
     *
     * @return the response
     */
    public Http.Response response() {
        return Http.Context.current().response();
    }

    /**
     * Returns the current HTTP session.
     *
     * @return the session
     */
    public Http.Session session() {
        return Http.Context.current().session();
    }

    /**
     * Puts a new value into the current session.
     *
     * @param key   the key to set into the session
     * @param value the value to set for <code>key</code>
     */
    public void session(String key, String value) {
        session().put(key, value);
    }

    /**
     * Returns a value from the session.
     *
     * @param key the session key
     * @return the value for the provided key, or null if there was no value
     */
    public String session(String key) {
        return session().get(key);
    }

    /**
     * Returns the current HTTP flash scope.
     *
     * @return the flash scope
     */
    public Http.Flash flash() {
        return Http.Context.current().flash();
    }

    /**
     * Puts a new value into the flash scope.
     *
     * @param key   the key to put into the flash scope
     * @param value the value corresponding to <code>key</code>
     */
    public void flash(String key, String value) {
        flash().put(key, value);
    }

    /**
     * Returns a value from the flash scope.
     *
     * @param key the key to look up in the flash scope
     * @return the value corresponding to <code>key</code> from the flash scope, or null if there was none
     */
    public String flash(String key) {
        return flash().get(key);
    }
}