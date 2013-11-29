/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 *
 */
public interface WSResponse {

    public Object getUnderlying();

    int getStatus();

    String getStatusText();

    String getHeader(String key);

    List<WSCookie> getCookies();

    WSCookie getCookie(String name);

    String getBody();

    Document asXml();

    JsonNode asJson();

    InputStream getBodyAsStream();

    byte[] asByteArray();

    URI getUri();
}
