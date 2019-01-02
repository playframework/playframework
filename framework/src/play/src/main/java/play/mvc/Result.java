/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import play.api.mvc.DiscardingCookie;
import play.core.j.JavaHelpers$;
import play.core.j.JavaResultExtractor;
import play.http.HttpEntity;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.libs.Scala;
import scala.Option;

import static play.mvc.Http.Cookie;
import static play.mvc.Http.Cookies;
import static play.mvc.Http.Flash;
import static play.mvc.Http.HeaderNames.LOCATION;
import static play.mvc.Http.Session;

/**
 * Any action result.
 */
public class Result {

    /** Statically compiled pattern for extracting the charset from a Result.  */
    private static final Pattern SPLIT_CHARSET = Pattern.compile("(?i);\\s*charset=");

    private final ResponseHeader header;
    private final HttpEntity body;
    private final Flash flash;
    private final Session session;
    private final List<Cookie> cookies;

    /**
     * Create a result from a Scala ResponseHeader and a body.
     *
     * @param header  the response header
     * @param body    the response body.
     * @param session the session set on the response.
     * @param flash   the flash object on the response.
     * @param cookies the cookies set on the response.
     */
    public Result(ResponseHeader header, HttpEntity body, Session session, Flash flash, List<Cookie> cookies) {
        this.header = header;
        this.body = body;
        this.session = session;
        this.flash = flash;
        this.cookies = cookies;
    }

    /**
     * Create a result from a Scala ResponseHeader and a body.
     *
     * @param header the response header
     * @param body the response body.
     */
    public Result(ResponseHeader header, HttpEntity body) {
        this(header, body, null, null, Collections.emptyList());
    }

    /**
     * Create a result.
     *
     * @param status The status.
     * @param reasonPhrase The reason phrase, if a non default reason phrase is required.
     * @param headers The headers.
     * @param body The body.
     */
    public Result(int status, String reasonPhrase, Map<String, String> headers, HttpEntity body) {
        this(new ResponseHeader(status, headers, reasonPhrase), body);
    }

    /**
     * Create a result.
     *
     * @param status The status.
     * @param headers The headers.
     * @param body The body.
     */
    public Result(int status, Map<String, String> headers, HttpEntity body) {
        this(status, (String)null, headers, body);
    }

    /**
     * Create a result with no body.
     *
     * @param status The status.
     * @param headers The headers.
     */
    public Result(int status, Map<String, String> headers) {
        this(status, (String)null, headers, HttpEntity.NO_ENTITY);
    }

    /**
     * Create a result.
     *
     * @param status The status.
     * @param body The entity.
     */
    public Result(int status, HttpEntity body) {
        this(status, (String)null, Collections.emptyMap(), body);
    }

    /**
     * Create a result with no entity.
     *
     * @param status The status.
     */
    public Result(int status) {
        this(status, (String)null, Collections.emptyMap(), HttpEntity.NO_ENTITY);
    }

    /**
     * Get the status.
     *
     * @return the status
     */
    public int status() {
        return header.status();
    }

    /**
     * Get the reason phrase, if it was set.
     *
     * @return the reason phrase (e.g. "NOT FOUND")
     */
    public Optional<String> reasonPhrase() {
        return header.reasonPhrase();
    }

    /**
     * Get the response header
     *
     * @return the header
     */
    protected ResponseHeader getHeader() {
        return this.header;
    }

    /**
     * Get the body of this result.
     *
     * @return the body
     */
    public HttpEntity body() {
        return body;
    }

    /**
     * Extracts the Location header of this Result value if this Result is a Redirect.
     *
     * @return the location (if it was set)
     */
    public Optional<String> redirectLocation() {
        return header(LOCATION);
    }

    /**
     * Extracts an Header value of this Result value.
     *
     * @param header the header name.
     * @return the header (if it was set)
     */
    public Optional<String> header(String header) {
        return this.header.getHeader(header);
    }

    /**
     * Extracts all Headers of this Result value.
     *
     * The returned map is not modifiable.
     *
     * @return the immutable map of headers
     */
    public Map<String, String> headers() {
        return this.header.headers();
    }

    /**
     * Extracts the Content-Type of this Result value.
     *
     * @return the content type (if it was set)
     */
    public Optional<String> contentType() {
        return body.contentType().map(h -> {
            if (h.contains(";")) {
                return h.substring(0, h.indexOf(';')).trim();
            } else {
                return h.trim();
            }
        });
    }

    /**
     * Extracts the Charset of this Result value.
     *
     * @return the charset (if it was set)
     */
    public Optional<String> charset() {
        return body.contentType().flatMap(h -> {
            String[] parts = SPLIT_CHARSET.split(h, 2);
            if (parts.length > 1) {
                String charset = parts[1];
                return Optional.of(charset.trim());
            } else {
                return Optional.empty();
            }
        });
    }

    /**
     * Extracts the Flash values of this Result value.
     *
     * @return the flash (if it was set)
     */
    public Flash flash() {
        return flash;
    }

    /**
     * Sets a new flash for this result, discarding the existing flash.
     *
     * @param flash the flash to set with this result
     * @return the new result
     */
    public Result withFlash(Flash flash) {
        play.api.mvc.Result.warnFlashingIfNotRedirect(flash.asScala(), header.asScala());
        return new Result(header, body, session, flash, cookies);
    }

    /**
     * Sets a new flash for this result, discarding the existing flash.
     *
     * @param flash the flash to set with this result
     * @return the new result
     */
    public Result withFlash(Map<String, String> flash) {
        return withFlash(new Flash(flash));
    }

    /**
     * Discards the existing flash for this result.
     *
     * @return the new result
     */
    public Result withNewFlash() {
        return withFlash(Collections.emptyMap());
    }

    /**
     * Adds values to the flash.
     *
     * @param values A map with values to add to this result's flash
     * @return A copy of this result with values added to its flash scope.
     */
    public Result flashing(Map<String, String> values) {
        if(this.flash == null) {
            return withFlash(values);
        } else {
            return withFlash(this.flash.adding(values));
        }
    }

    /**
     * Adds the given key and value to the flash.
     *
     * @param key The key to add to this result's flash
     * @param value The value to add to this result's flash
     * @return A copy of this result with the key and value added to its flash scope.
     */
    public Result flashing(String key, String value) {
        Map<String, String> newValues = new HashMap<>(1);
        newValues.put(key, value);
        return flashing(newValues);
    }

    /**
     * Removes values from the flash.
     *
     * @param keys Keys to remove from flash
     * @return A copy of this result with keys removed from its flash scope.
     */
    public Result removingFromFlash(String... keys) {
        if(this.flash == null) {
            return withNewFlash();
        }
        return withFlash(this.flash.removing(keys));
    }

    /**
     * Extracts the Session of this Result value.
     *
     * @return the session (if it was set)
     */
    public Session session() {
        return session;
    }

    /**
     * @param request Current request
     * @return The session carried by this result. Reads the given request's session if this result does not has a session.
     */
    public Session session(Http.Request request) {
        if(session != null) {
            return session;
        } else {
            return request.session();
        }
    }

    /**
     * Sets a new session for this result, discarding the existing session.
     *
     * @param session the session to set with this result
     * @return the new result
     */
    public Result withSession(Session session) {
        return new Result(header, body, session, flash, cookies);
    }

    /**
     * Sets a new session for this result, discarding the existing session.
     *
     * @param session the session to set with this result
     * @return the new result
     */
    public Result withSession(Map<String, String> session) {
        return withSession(new Session(session));
    }

    /**
     * Discards the existing session for this result.
     *
     * @return the new result
     */
    public Result withNewSession() {
        return withSession(Collections.emptyMap());
    }

    /**
     * Adds values to the session.
     *
     * @param values A map with values to add to this result's session
     * @return A copy of this result with values added to its session scope.
     */
    public Result addingToSession(Http.Request request, Map<String, String> values) {
        return withSession(session(request).adding(values));
    }

    /**
     * Adds the given key and value to the session.
     *
     * @param key The key to add to this result's session
     * @param value The value to add to this result's session
     * @return A copy of this result with the key and value added to its session scope.
     */
    public Result addingToSession(Http.Request request, String key, String value) {
        Map<String, String> newValues = new HashMap<>(1);
        newValues.put(key, value);
        return addingToSession(request, newValues);
    }

    /**
     * Removes values from the session.
     *
     * @param keys Keys to remove from session
     * @return A copy of this result with keys removed from its session scope.
     */
    public Result removingFromSession(Http.Request request, String... keys) {
        return withSession(session(request).removing(keys));
    }

    /**
     * Extracts a Cookie value from this Result value
     *
     * @param name the cookie's name.
     * @return the cookie (if it was set)
     *
     * @deprecated Deprecated as of 2.7.0. Use {@link #getCookie(String)}
     */
    @Deprecated
    public Cookie cookie(String name) {
        return cookies().get(name);
    }

    /**
     * Extracts a Cookie value from this Result value
     *
     * @param name the cookie's name.
     * @return the optional cookie
     */
    public Optional<Cookie> getCookie(String name) {
        return cookies().getCookie(name);
    }

    /**
     * Extracts the Cookies (an iterator) from this result value.
     *
     * @return the cookies (if they were set)
     */
    public Cookies cookies() {
        return new Cookies() {
            @Override
            public Optional<Cookie> getCookie(String name) {
                return cookies.stream().filter(c -> c.name().equals(name)).findFirst();
            }

            @Override
            public Iterator<Cookie> iterator() {
                return cookies.iterator();
            }
        };
    }

    /**
     * Returns a copy of this result with the given cookies.
     * @param newCookies the cookies to add to the result.
     * @return the transformed copy.
     */
    public Result withCookies(Cookie... newCookies) {
        List<Cookie> finalCookies = Stream.concat(cookies.stream().filter(cookie -> {
            for (Cookie newCookie : newCookies) {
                if (cookie.name().equals(newCookie.name())) return false;
            }
            return true;
        }), Stream.of(newCookies)).collect(Collectors.toList());
        return new Result(header, body, session, flash, finalCookies);
    }

    /**
     * Discard a cookie on the default path ("/") with no domain and that's not secure.
     *
     * @param name The name of the cookie to discard, must not be null
     */
    public Result discardingCookie(String name) {
        return discardingCookie(name, "/", null, false);
    }

    /**
     * Discard a cookie on the given path with no domain and not that's secure.
     *
     * @param name The name of the cookie to discard, must not be null
     * @param path The path of the cookie to discard, may be null
     */
    public Result discardingCookie(String name, String path) {
        return discardingCookie(name, path, null, false);
    }

    /**
     * Discard a cookie on the given path and domain that's not secure.
     *
     * @param name The name of the cookie to discard, must not be null
     * @param path The path of the cookie te discard, may be null
     * @param domain The domain of the cookie to discard, may be null
     */
    public Result discardingCookie(String name, String path, String domain) {
        return discardingCookie(name, path, domain, false);
    }

    /**
     * Discard a cookie in this result
     *
     * @param name The name of the cookie to discard, must not be null
     * @param path The path of the cookie te discard, may be null
     * @param domain The domain of the cookie to discard, may be null
     * @param secure Whether the cookie to discard is secure
     */
    public Result discardingCookie(String name, String path, String domain, boolean secure) {
        return withCookies(new DiscardingCookie(name, path, Option.apply(domain), secure).toCookie().asJava());
    }

    /**
     * Return a copy of this result with the given header.
     *
     * @param name the header name
     * @param value the header value
     * @return the transformed copy
     */
    public Result withHeader(String name, String value) {
        return new Result(header.withHeader(name, value), body, session, flash, cookies);
    }

    /**
     * Return a copy of this result with the given headers.
     *
     * The headers are processed in pairs, so nameValues(0) is the first header's name, and
     * nameValues(1) is the first header's value, nameValues(2) is second header's name,
     * and so on.
     *
     * @param nameValues the array of names and values.
     * @return the transformed copy
     */
    public Result withHeaders(String... nameValues) {
        return new Result(JavaResultExtractor.withHeader(header, nameValues), body, session, flash, cookies);
    }

    /**
     * Discard a HTTP header in this result.
     *
     * @param name the header name
     * @return the transformed copy
     */
    public Result withoutHeader(String name) {
        return new Result(header.withoutHeader(name), body, session, flash, cookies);
    }

    /**
     * Return a copy of the result with a different Content-Type header.
     *
     * @param contentType the content type to set
     * @return the transformed copy
     */
    public Result as(String contentType) {
        return new Result(header, body.as(contentType));
    }

    /**
     * Returns a new result with the given lang set in a cookie. For example:
     *
     * <pre>
     * {@code
     * public Result action() {
     *     ok("Hello").withLang(Lang.forCode("es"), messagesApi);
     * }
     * }
     * </pre>
     *
     * Where {@code messagesApi} were injected.
     *
     * @param lang the new lang
     * @param messagesApi the messages api implementation
     * @return a new result with the given lang.
     *
     * @see MessagesApi#setLang(Result, Lang)
     */
    public Result withLang(Lang lang, MessagesApi messagesApi) {
        return messagesApi.setLang(this, lang);
    }

    /**
     * Clears the lang cookie from this result. For example:
     *
     * <pre>
     * {@code
     * public Result action() {
     *     ok("Hello").withoutLang(messagesApi);
     * }
     * }
     * </pre>
     *
     * Where {@code messagesApi} were injected.
     *
     * @param messagesApi the messages api implementation
     * @return a new result without the lang
     *
     * @see MessagesApi#clearLang(Result)
     */
    public Result withoutLang(MessagesApi messagesApi) {
        return messagesApi.clearLang(this);
    }

    /**
     * Convert this result to a Scala result.
     *
     * @return the Scala result.
     */
    public play.api.mvc.Result asScala() {
        return new play.api.mvc.Result(
            header.asScala(),
            body.asScala(),
            session == null ? Scala.None() : Scala.Option(play.api.mvc.Session.fromJavaSession(session)),
            flash == null ? Scala.None() : Scala.Option(play.api.mvc.Flash.fromJavaFlash(flash)),
            JavaHelpers$.MODULE$.cookiesToScalaCookies(cookies)
        );
    }
}
