/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

public interface PlayOutputHeader extends PlayHasHeaders {
    public int getHeaderSize();
    public PlayHttpResponseStatus getStatus();
    public PlayHttpVersion getProtocolVersion();
}
