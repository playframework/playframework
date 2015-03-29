/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package play.libs.ws.ning;


import com.fasterxml.jackson.databind.JsonNode;
import com.ning.http.util.AsyncHttpProviderUtils;
import org.w3c.dom.Document;
import play.libs.Json;

import play.libs.ws.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A WS response.
 */
public class NingWSResponse implements WSResponse {

    private com.ning.http.client.Response ahcResponse;

    public NingWSResponse(com.ning.http.client.Response ahcResponse) {
        this.ahcResponse = ahcResponse;
    }

    @Override
    public Object getUnderlying() {
        return this.ahcResponse;
    }

    /**
     * Get the HTTP status code of the response
     */
    @Override
    public int getStatus() {
        return ahcResponse.getStatusCode();
    }

    /**
     * Get the HTTP status text of the response
     */
    @Override
    public String getStatusText() {
        return ahcResponse.getStatusText();
    }

    /**
     * Get all the HTTP headers of the response as a case-insensitive map
     */
    @Override
    public Map<String, List<String>> getAllHeaders() {
        return ahcResponse.getHeaders();
    }

    /**
     * Get the given HTTP header of the response
     */
    @Override
    public String getHeader(String key) {
        return ahcResponse.getHeader(key);
    }

    /**
     * Get all the cookies.
     */
    @Override
    public List<WSCookie> getCookies() {
        List<WSCookie> cookieList = new ArrayList<WSCookie>();
        for (com.ning.http.client.cookie.Cookie ahcCookie : ahcResponse.getCookies()) {
            cookieList.add(new NingWSCookie(ahcCookie));
        }
        return cookieList;
    }

    /**
     * Get only one cookie, using the cookie name.
     */
    @Override
    public WSCookie getCookie(String name) {
        for (com.ning.http.client.cookie.Cookie ahcCookie : ahcResponse.getCookies()) {
            // safe -- cookie.getName() will never return null
            if (ahcCookie.getName().equals(name)) {
                return new NingWSCookie(ahcCookie);
            }
        }
        return null;
    }

    /**
     * Get the response body as a string.  If the charset is not specified, this defaults to ISO-8859-1 for text
     * sub mime types, as per RFC-2616 sec 3.7.1, otherwise it defaults to UTF-8.
     */
    @Override
    public String getBody() {
        try {
            // RFC-2616#3.7.1 states that any text/* mime type should default to ISO-8859-1 charset if not
            // explicitly set, while Plays default encoding is UTF-8.  So, use UTF-8 if charset is not explicitly
            // set and content type is not text/*, otherwise default to ISO-8859-1
            String contentType = ahcResponse.getContentType();
            if (contentType == null) {
                // As defined by RFC-2616#7.2.1
                contentType = "application/octet-stream";
            }
            String charset = AsyncHttpProviderUtils.parseCharset(contentType);

            if (charset != null) {
                return ahcResponse.getResponseBody(charset);
            } else if (contentType.startsWith("text/")) {
                return ahcResponse.getResponseBody(AsyncHttpProviderUtils.DEFAULT_CHARSET.toString());
            } else {
                return ahcResponse.getResponseBody("utf-8");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the response body as a {@link org.w3c.dom.Document DOM document}
     * @return a DOM document
     */
    @Override
    public Document asXml() {
        try {
            return play.libs.XML.fromInputStream(ahcResponse.getResponseBodyAsStream(), "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the response body as a {@link com.fasterxml.jackson.databind.JsonNode}
     * @return the json response
     */
    @Override
    public JsonNode asJson() {
        try {
            // Jackson will automatically detect the correct encoding according to the rules in RFC-4627
            return Json.parse(ahcResponse.getResponseBodyAsStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the response body as a stream
     * @return The stream to read the response body from
     */
    @Override
    public InputStream getBodyAsStream() {
        try {
            return ahcResponse.getResponseBodyAsStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the response body as a byte array
     * @return The byte array
     */
    @Override
    public byte[] asByteArray() {
        try {
            return ahcResponse.getResponseBodyAsBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the request {@link java.net.URI}. Note that if the request got redirected, the value of the
     * {@link java.net.URI} will be the last valid redirect url.
     *
     * @return the request {@link java.net.URI}.
     */
    @Override
    public URI getUri() {
        try {
            return ahcResponse.getUri().toJavaNetURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
