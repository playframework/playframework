/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.openid;

import java.util.Collections;
import java.util.Map;

/**
 * The OpenID user info
 */
public class UserInfo {

    private final String id;
    private final Map<String, String> attributes;

    public UserInfo(String id) {
        this.id = id;
        this.attributes = Collections.emptyMap();
    }

    public UserInfo(String id, Map<String, String> attributes) {
        this.id = id;
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public String id() {
        return id;
    }

    public Map<String, String> attributes() {
        return attributes;
    }
}
