/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import play.api.mvc.ResponseHeader;
import play.core.j.JavaResultExtractor;
import play.http.HttpEntity;
import scala.collection.JavaConversions;
import scala.compat.java8.OptionConverters;

import static play.mvc.Http.HeaderNames.*;

import static play.mvc.Http.*;

/**
 * Any action result.
 */
public class Result {

    private final ResponseHeader header;
    private final HttpEntity body;

    /**
     * Create a result from a Scala ResponseHeader and a body.
     */
    public Result(ResponseHeader header, HttpEntity body) {
        this.header = header;
        this.body = body;
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
            if (h.contains("; charset=")) {
                return Optional.of(h.substring(h.indexOf("; charset=") + 10, h.length()).trim());
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
        return JavaResultExtractor.getFlash(header);
    }

    /**
     * Extracts the Session of this Result value.
     *
     * @return the session (if it was set)
     */
    public Session session() {
        return JavaResultExtractor.getSession(header);
    }

    /**
     * Extracts a Cookie value from this Result value
     *
     * @return the cookie (if it was set)
     */
    public Cookie cookie(String name) {
        return JavaResultExtractor.getCookies(header).get(name);
    }

    /**
     * Extracts the Cookies (an iterator) from this result value.
     *
     * @return the cookies (if they were set)
     */
    public Cookies cookies() {
        return JavaResultExtractor.getCookies(header);
    }

    /**
     * Return a copy of this result with the given header.
     *
     * @return the transformed copy
     */
    public Result withHeader(String name, String value) {
        return new Result(JavaResultExtractor.withHeader(header, name, value), body);
    }

    /**
     * Return a copy of this result with the given headers.
     *
     * @return the transformed copy
     */
    public Result withHeaders(String... nameValues) {
        return new Result(JavaResultExtractor.withHeader(header, nameValues), body);
    }

    /**
     * Return a copy of the result with a different Content-Type header.
     *
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
        return new play.api.mvc.Result(header, body.asScala());
    }
}
