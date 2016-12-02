/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

/**
 * A WS Cookie.
 */
public interface WSCookie {

    /**
     * Returns the underlying "native" object for the cookie.
     *
     * This is probably an <code>org.asynchttpclient.cookie.Cookie</code>.
     *
     * @return the "native" object
     */
    public Object getUnderlying();

    public String getDomain();

    public String getName();

    public String getValue();

    public String getPath();

    public Long getMaxAge();

    public Boolean isSecure();

    // Cookie ports should not be used; cookies for a given host are shared across
    // all the ports on that host.
}
