/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import scala.compat.java8.OptionConverters;

import java.util.*;

/**
 * A simple HTTP response header, used for standard responses.
 *
 * @see play.mvc.Result
 * @see play.api.mvc.ResponseHeader
 */
public class ResponseHeader {

    private final int status;
    private final String reasonPhrase;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public ResponseHeader(int status, Map<String, String> headers) {
        this(status, headers, null);
    }

    public ResponseHeader(int status, Map<String, String> headers, String reasonPhrase) {
        this.status = status;
        this.reasonPhrase = reasonPhrase;
        this.headers.putAll(headers);
    }

    public play.api.mvc.ResponseHeader asScala() {
        return new play.api.mvc.ResponseHeader(status, headers, OptionConverters.toScala(Optional.ofNullable(reasonPhrase)));
    }

    public int status() {
        return status;
    }

    public Optional<String> reasonPhrase() {
        return Optional.ofNullable(reasonPhrase);
    }

    public Optional<String> getHeader(String headerName) {
        return Optional.ofNullable(this.headers.get(headerName));
    }

    public Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }

    public ResponseHeader withoutHeader(String name) {
        Map<String, String> updatedHeaders = copyCurrentHeaders();
        updatedHeaders.remove(name);
        return new ResponseHeader(status, updatedHeaders, reasonPhrase);
    }

    public ResponseHeader withHeader(String name, String value) {
        Map<String, String> updatedHeaders = copyCurrentHeaders();
        updatedHeaders.put(name, value);
        return new ResponseHeader(status, updatedHeaders, reasonPhrase);
    }

    public ResponseHeader withHeaders(Map<String, String> newHeaders) {
        Map<String, String> updatedHeaders = copyCurrentHeaders();
        updatedHeaders.putAll(newHeaders);
        return new ResponseHeader(status, updatedHeaders, reasonPhrase);
    }

    private Map<String, String> copyCurrentHeaders() {
        Map<String, String> updatedHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        updatedHeaders.putAll(this.headers);
        return updatedHeaders;
    }
}
