/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jim on 11/20/13.
 */
public interface PlayHasHeaders {
    public String getHeader(String name);
    public List<String> getHeaders(String name);
    public List<Map.Entry<String, String>> getHeaders();
    public boolean containsHeader(String name);
    public Set<String> getHeaderNames();
}
