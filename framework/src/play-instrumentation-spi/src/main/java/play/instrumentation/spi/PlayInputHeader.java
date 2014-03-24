/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

public interface PlayInputHeader extends PlayHasHeaders {
    public int getHeaderSize();
    public PlayHttpMethod getMethod();
    public String getUri();
    public PlayHttpVersion getProtocolVersion();
}
