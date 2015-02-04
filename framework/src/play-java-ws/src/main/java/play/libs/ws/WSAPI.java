/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;


public interface WSAPI {

    public WSClient client();

    public WSRequest url(String url);

}
