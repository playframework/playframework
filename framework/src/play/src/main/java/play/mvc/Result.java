/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import play.core.j.JavaHelpers$;
import play.core.j.JavaResultExtractor;
import play.http.HttpEntity;
import play.libs.Scala;

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
                return h.substring(0, h.indexOf(";")).trim();
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
     * Extracts the Session of this Result value.
     *
     * @return the session (if it was set)
     */
    public Session session() {
        return session;
    }

    /**
     * Extracts a Cookie value from this Result value
     *
     * @param name the cookie's name.
     * @return the cookie (if it was set)
     */
    public Cookie cookie(String name) {
        return cookies().get(name);
    }

    /**
     * Extracts the Cookies (an iterator) from this result value.
     *
     * @return the cookies (if they were set)
     */
    public Cookies cookies() {
        return new Cookies() {
            @Override
            public Cookie get(String name) {
                return cookies.stream().filter(c -> c.name().equals(name)).findFirst().get();
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
     * Return a copy of the result with a different Content-Type header.
     *
     * @param contentType the content type to set
     * @return the transformed copy
     */
    public Result as(String contentType) {
        return new Result(header, body.as(contentType));
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
