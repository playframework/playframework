/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


public interface WSClient {

    public Object getUnderlying();

    WSRequestHolder url(String url);

}