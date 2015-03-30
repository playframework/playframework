/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.mvc;

import java.util.Map;

import play.core.j.JavaResultExtractor;
import play.mvc.Http.HeaderNames;

import static play.mvc.Http.*;

/**
 * Any action result.
 */
public interface Result extends HeaderNames {
    
    /**
     * Retrieves the real (Scala-based) result.
     */
    play.api.mvc.Result toScala();


    /**
     * Extracts the Status code of this Result value.
     */
    default int status() {
        return toScala().header().status();
    }

    /**
     * Extracts the Location header of this Result value if this Result is a Redirect.
     */
    default String redirectLocation() {
        return header(LOCATION);
    }

    /**
     * Extracts an Header value of this Result value.
     */
    default String header(String header) {
        return JavaResultExtractor.getHeaders(this).get(header);
    }

    /**
     * Extracts all Headers of this Result value.
     */
    default Map<String, String> headers() {
        return JavaResultExtractor.getHeaders(this);
    }

    /**
     * Extracts the Content-Type of this Result value.
     */
    default String contentType() {
        String h = header(CONTENT_TYPE);
        if(h == null) return null;
        if(h.contains(";")) {
            return h.substring(0, h.indexOf(";")).trim();
        } else {
            return h.trim();
        }
    }

    /**
     * Extracts the Charset of this Result value.
     */
    default String charset() {
        String h = header(CONTENT_TYPE);
        if(h == null) return null;
        if(h.contains("; charset=")) {
            return h.substring(h.indexOf("; charset=") + 10, h.length()).trim();
        } else {
            return null;
        }
    }

    /**
     * Extracts the Flash values of this Result value.
     */
    default Flash flash() {
        return JavaResultExtractor.getFlash(this);
    }

    /**
     * Extracts the Session of this Result value.
     */
    default Session session() {
        return JavaResultExtractor.getSession(this);
    }

    /**
     * Extracts a Cookie value from this Result value
     */
    default Cookie cookie(String name) {
        return JavaResultExtractor.getCookies(this).get(name);
    }

    /**
     * Extracts the Cookies (an iterator) from this result value.
     */
    default Cookies cookies() {
        return play.core.j.JavaResultExtractor.getCookies(this);
    }

}
