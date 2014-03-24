/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

import java.util.HashMap;
import java.util.Map;

public class PlayHttpMethod implements Comparable<PlayHttpMethod> {
    public static final PlayHttpMethod OPTIONS = new PlayHttpMethod("OPTIONS");
    public static final PlayHttpMethod GET = new PlayHttpMethod("GET");
    public static final PlayHttpMethod HEAD = new PlayHttpMethod("HEAD");
    public static final PlayHttpMethod POST = new PlayHttpMethod("POST");
    public static final PlayHttpMethod PUT = new PlayHttpMethod("PUT");
    public static final PlayHttpMethod PATCH = new PlayHttpMethod("PATCH");
    public static final PlayHttpMethod DELETE = new PlayHttpMethod("DELETE");
    public static final PlayHttpMethod TRACE = new PlayHttpMethod("TRACE");
    public static final PlayHttpMethod CONNECT = new PlayHttpMethod("CONNECT");

    private static final Map<String, PlayHttpMethod> methodMap =
            new HashMap<String, PlayHttpMethod>();

    static {
        methodMap.put(OPTIONS.toString(), OPTIONS);
        methodMap.put(GET.toString(), GET);
        methodMap.put(HEAD.toString(), HEAD);
        methodMap.put(POST.toString(), POST);
        methodMap.put(PUT.toString(), PUT);
        methodMap.put(PATCH.toString(), PATCH);
        methodMap.put(DELETE.toString(), DELETE);
        methodMap.put(TRACE.toString(), TRACE);
        methodMap.put(CONNECT.toString(), CONNECT);
    }

    public static PlayHttpMethod valueOf(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        name = name.trim();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }

        PlayHttpMethod result = methodMap.get(name);
        if (result != null) {
            return result;
        } else {
            return new PlayHttpMethod(name);
        }
    }

    private final String name;

    public PlayHttpMethod(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        name = name.trim();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }

        for (int i = 0; i < name.length(); i ++) {
            if (Character.isISOControl(name.charAt(i)) ||
                    Character.isWhitespace(name.charAt(i))) {
                throw new IllegalArgumentException("invalid character in name");
            }
        }

        this.name = name;
    }

    /**
     * Returns the name of this method.
     */
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayHttpMethod)) {
            return false;
        }

        PlayHttpMethod that = (PlayHttpMethod) o;
        return getName().equals(that.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    public int compareTo(PlayHttpMethod o) {
        return getName().compareTo(o.getName());
    }
}
