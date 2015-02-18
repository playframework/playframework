/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


/**
 * A WS Cookie.
 */
public interface WSCookie {

    /**
     * Returns the underlying "native" object for the cookie.
     */
    public Object getUnderlying();

    public String getDomain();

    public String getName();

    public String getValue();

    public String getPath();

    public Long getExpires();

    public Integer getMaxAge();

    public Boolean isSecure();

    // Cookie ports should not be used; cookies for a given host are shared across
    // all the ports on that host.
}
