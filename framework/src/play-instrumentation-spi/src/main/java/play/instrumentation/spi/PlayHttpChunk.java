/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

public interface PlayHttpChunk {
    public int getHeaderSize();
    public int getChunkSize();
}
