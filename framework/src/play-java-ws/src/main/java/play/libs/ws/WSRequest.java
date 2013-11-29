/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws;

import play.libs.F;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface WSRequest {

    F.Promise<WSResponse> execute();

    Map<String, List<String>> getAllHeaders();

    String getMethod();

    List<String> getHeader(String name);

    String getUrl();

    @Deprecated
    WSRequest setUrl(String url);

    @Deprecated
    WSRequest setHeader(String name, String value);

    @Deprecated
    WSRequest setHeaders(Map<String, Collection<String>> hdrs);

    // WSRequest<T> setHeaders(FluentCaseInsensitiveStringsMap hdrs);

    @Deprecated
    WSRequest addHeader(String name, String value);

    @Deprecated
    WSRequest auth(String username, String password, WSAuthScheme scheme);

}
