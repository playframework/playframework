/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import play.api.mvc.ResponseHeader;
import play.core.j.JavaHelpers$;
import play.core.j.JavaResultExtractor;
import play.http.HttpEntity;
import play.libs.Scala;
import scala.collection.JavaConversions;
import scala.compat.java8.OptionConverters;

import static play.mvc.Http.Cookie;
import static play.mvc.Http.Cookies;
import static play.mvc.Http.Flash;
import static play.mvc.Http.HeaderNames.LOCATION;
import static play.mvc.Http.Session;

/**
 * Any action result.
 */
public class Result {

    private final ResponseHeader header;
    private final HttpEntity body;
    private final Flash flash;
    private final Session session;
    private final List<Cookie> cookies;

    /**
     * Create a result from a Scala ResponseHeader and a body.
     *
     * @param header the response header
     * @param body the response body.
     * @param session    the session set on the response.
     * @param flash      the flash object on the response.
     * @param cookies    the cookies set on the response.
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
    public Result(int status, Optional<String> reasonPhrase, Map<String, String> headers, HttpEntity body) {
        this(new ResponseHeader(status, headers, OptionConverters.toScala(reasonPhrase)),
                body);
    }

    /**
     * Create a result.
     *
     * @param status The status.
     * @param headers The headers.
     * @param body The body.
     */
    public Result(int status, Map<String, String> headers, HttpEntity body) {
        this(status, Optional.empty(), headers, body);
    }

    /**
     * Create a result with no body.
     *
     * @param status The status.
     * @param headers The headers.
     */
    public Result(int status, Map<String, String> headers) {
        this(status, Optional.empty(), headers, HttpEntity.NO_ENTITY);
    }

    /**
     * Create a result.
     *
     * @param status The status.
     * @param body The entity.
     */
    public Result(int status, HttpEntity body) {
        this(status, Optional.empty(), Collections.emptyMap(), body);
    }

    /**
     * Create a result with no entity.
     *
     * @param status The status.
     */
    public Result(int status) {
        this(status, Optional.empty(), Collections.emptyMap(), HttpEntity.NO_ENTITY);
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
        return OptionConverters.toJava(header.reasonPhrase());
    }

    /**
     * Get the response header
     *
     * @return the header
     */
    protected ResponseHeader header() {
        return header;
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
        return OptionConverters.<String>toJava(this.header.headers().get(header));
    }

    /**
     * Extracts all Headers of this Result value.
     *
     * The returned map is not modifiable.
     *
     * @return the immutable map of headers
     */
    public Map<String, String> headers() {
        return Collections.unmodifiableMap(JavaConversions.mapAsJavaMap(this.header.headers()));
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
            String[] parts = h.split("(?i);\\s*charset=", 2);
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
     * @param cookies the cookies to add to the result.
     * @return the transformed copy.
     */
    public Result withCookies(Cookie... cookies) {
        return new Result(header, body, session, flash, Arrays.asList(cookies));
    }

    /**
     * Return a copy of this result with the given header.
     *
     * @param name the header name
     * @param value the header value
     * @return the transformed copy
     */
    public Result withHeader(String name, String value) {
        return new Result(JavaResultExtractor.withHeader(header, name, value), body, session, flash, cookies);
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
            header,
            body.asScala(),
            session == null ? Scala.None() : Scala.Option(play.api.mvc.Session.fromJavaSession(session)),
            flash == null ? Scala.None() : Scala.Option(play.api.mvc.Flash.fromJavaFlash(flash)),
            JavaHelpers$.MODULE$.cookiesToScalaCookies(cookies)
        );
    }
}
