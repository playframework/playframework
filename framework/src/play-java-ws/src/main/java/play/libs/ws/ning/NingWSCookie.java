/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;

import play.libs.ws.*;

/**
 * The Ning implementation of a WS cookie.
 */
public class NingWSCookie implements WSCookie {

    private final org.asynchttpclient.cookie.Cookie ahcCookie;

    public NingWSCookie(org.asynchttpclient.cookie.Cookie ahcCookie) {
        this.ahcCookie = ahcCookie;
    }

    /**
     * Returns the underlying "native" object for the cookie.
     */
    public Object getUnderlying() {
        return ahcCookie;
    }

    public String getDomain() {
        return ahcCookie.getDomain();
    }

    public String getName() {
        return ahcCookie.getName();
    }

    public String getValue() {
        return ahcCookie.getValue();
    }

    public String getPath() {
        return ahcCookie.getPath();
    }

    public Long getMaxAge() {
        return ahcCookie.getMaxAge();
    }

    public Boolean isSecure() {
        return ahcCookie.isSecure();
    }
}
