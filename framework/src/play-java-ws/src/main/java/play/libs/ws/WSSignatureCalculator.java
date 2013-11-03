/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

/**
 * Sign a WS call.
 */
public interface WSSignatureCalculator {

    /**
     * Sign a request
     */
    public void sign(WSRequest request);

}