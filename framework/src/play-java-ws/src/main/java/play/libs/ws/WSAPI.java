/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


public interface WSAPI {

    WSClient client();

    WSRequest url(String url);

}
