/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


public interface WSAPI {

    WSClient client();

    WSRequest url(String url);

}
