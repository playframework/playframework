/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package play.core.cookie.encoding;

import static play.core.cookie.encoding.CookieUtil.validateAttributeValue;

/**
 * The default {@link Cookie} implementation.
 */
public class DefaultCookie implements Cookie {

    private final String name;
    private String value;
    private boolean wrap;
    private String domain;
    private String path;
    private int maxAge = Integer.MIN_VALUE;
    private boolean secure;
    private boolean httpOnly;
    private String sameSite;

    /**
     * Creates a new cookie with the specified name and value.
     * @param name     The cookie's name
     * @param value    The cookie's value.
     */
    public DefaultCookie(String name, String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        name = name.trim();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }
        this.name = name;
        setValue(value);
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        this.value = value;
    }

    public boolean wrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public String domain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = validateAttributeValue("domain", domain);
    }

    public String path() {
        return path;
    }

    public void setPath(String path) {
        this.path = validateAttributeValue("path", path);
    }

    public int maxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String sameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    @Override
    public int hashCode() {
        return name().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Cookie)) {
            return false;
        }

        Cookie that = (Cookie) o;
        if (!name().equalsIgnoreCase(that.name())) {
            return false;
        }

        if (path() == null) {
            if (that.path() != null) {
                return false;
            }
        } else if (that.path() == null) {
            return false;
        } else if (!path().equals(that.path())) {
            return false;
        }

        if (domain() == null) {
            if (that.domain() != null) {
                return false;
            }
        } else if (that.domain() == null) {
            return false;
        } else {
            return domain().equalsIgnoreCase(that.domain());
        }

        if (sameSite() == null) {
            if (that.sameSite() != null) {
                return false;
            }
        } else if (that.sameSite() == null) {
            return false;
        } else {
            return sameSite().equalsIgnoreCase(that.sameSite());
        }

        return true;
    }

    public int compareTo(Cookie c) {
        int v = name().compareToIgnoreCase(c.name());
        if (v != 0) {
            return v;
        }

        if (path() == null) {
            if (c.path() != null) {
                return -1;
            }
        } else if (c.path() == null) {
            return 1;
        } else {
            v = path().compareTo(c.path());
            if (v != 0) {
                return v;
            }
        }

        if (domain() == null) {
            if (c.domain() != null) {
                return -1;
            }
        } else if (c.domain() == null) {
            return 1;
        } else {
            v = domain().compareToIgnoreCase(c.domain());
            return v;
        }

        return 0;
    }

    /**
     * Validate a cookie attribute value, throws a {@link IllegalArgumentException} otherwise.
     * Only intended to be used by {@link DefaultCookie}.
     * @param name attribute name
     * @param value attribute value
     * @return the trimmed, validated attribute value
     * @deprecated CookieUtil is package private, will be removed once old Cookie API is dropped
     */
    @Deprecated
    protected String validateValue(String name, String value) {
        return validateAttributeValue(name, value);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder()
            .append(name())
            .append('=')
            .append(value());
        if (domain() != null) {
            buf.append(", domain=")
               .append(domain());
        }
        if (path() != null) {
            buf.append(", path=")
               .append(path());
        }
        if (maxAge() >= 0) {
            buf.append(", maxAge=")
               .append(maxAge())
               .append('s');
        }
        if (isSecure()) {
            buf.append(", secure");
        }
        if (isHttpOnly()) {
            buf.append(", HTTPOnly");
        }
        if (sameSite() != null) {
            buf.append(", SameSite=" + sameSite);
        }
        return buf.toString();
    }
}
