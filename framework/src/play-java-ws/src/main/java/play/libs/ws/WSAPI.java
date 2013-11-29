/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


public interface WSAPI {

    public WSClient client();

    public WSRequestHolder url(String url);

}