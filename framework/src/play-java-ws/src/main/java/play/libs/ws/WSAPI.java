/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;


public interface WSAPI {

    WSClient client();

    WSRequest url(String url);

}
