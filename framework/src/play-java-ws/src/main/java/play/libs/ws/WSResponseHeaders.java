/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

import java.util.List;
import java.util.Map;

public interface WSResponseHeaders {
    int getStatus();

    Map<String, List<String>> getHeaders();
}

class DefaultWSResponseHeaders implements WSResponseHeaders {

    private final int status;
    private final Map<String, List<String>> headers;

    public DefaultWSResponseHeaders(int status, Map<String, List<String>> headers) {
        this.status = status;
        this.headers = headers;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    // hashcode and equals impl were generated with Eclipse

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
        result = prime * result + status;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultWSResponseHeaders other = (DefaultWSResponseHeaders) obj;
        if (headers == null) {
            if (other.headers != null)
                return false;
        } else if (!headers.equals(other.headers))
            return false;
        if (status != other.status)
            return false;
        return true;
    }
}
