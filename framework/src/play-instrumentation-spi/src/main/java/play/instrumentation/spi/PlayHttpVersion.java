/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayHttpVersion {
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(\\S+)/(\\d+)\\.(\\d+)");

    public static final PlayHttpVersion HTTP_1_0 = new PlayHttpVersion("HTTP", 1, 0, false);

    public static final PlayHttpVersion HTTP_1_1 = new PlayHttpVersion("HTTP", 1, 1, true);

    public static PlayHttpVersion valueOf(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }

        text = text.trim().toUpperCase();
        if ("HTTP/1.1".equals(text)) {
            return HTTP_1_1;
        }
        if ("HTTP/1.0".equals(text)) {
            return HTTP_1_0;
        }
        return new PlayHttpVersion(text, true);
    }

    private final String protocolName;
    private final int majorVersion;
    private final int minorVersion;
    private final String text;
    private final boolean keepAliveDefault;

    public PlayHttpVersion(String text, boolean keepAliveDefault) {
        if (text == null) {
            throw new NullPointerException("text");
        }

        text = text.trim().toUpperCase();
        if (text.length() == 0) {
            throw new IllegalArgumentException("empty text");
        }

        Matcher m = VERSION_PATTERN.matcher(text);
        if (!m.matches()) {
            throw new IllegalArgumentException("invalid version format: " + text);
        }

        protocolName = m.group(1);
        majorVersion = Integer.parseInt(m.group(2));
        minorVersion = Integer.parseInt(m.group(3));
        this.text = protocolName + '/' + majorVersion + '.' + minorVersion;
        this.keepAliveDefault = keepAliveDefault;
    }

    public PlayHttpVersion(
            String protocolName, int majorVersion, int minorVersion,
            boolean keepAliveDefault) {
        if (protocolName == null) {
            throw new NullPointerException("protocolName");
        }

        protocolName = protocolName.trim().toUpperCase();
        if (protocolName.length() == 0) {
            throw new IllegalArgumentException("empty protocolName");
        }

        for (int i = 0; i < protocolName.length(); i ++) {
            if (Character.isISOControl(protocolName.charAt(i)) ||
                    Character.isWhitespace(protocolName.charAt(i))) {
                throw new IllegalArgumentException("invalid character in protocolName");
            }
        }

        if (majorVersion < 0) {
            throw new IllegalArgumentException("negative majorVersion");
        }
        if (minorVersion < 0) {
            throw new IllegalArgumentException("negative minorVersion");
        }

        this.protocolName = protocolName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        text = protocolName + '/' + majorVersion + '.' + minorVersion;
        this.keepAliveDefault = keepAliveDefault;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public String getText() {
        return text;
    }

    public boolean isKeepAliveDefault() {
        return keepAliveDefault;
    }

    @Override
    public String toString() {
        return getText();
    }

    @Override
    public int hashCode() {
        return (getProtocolName().hashCode() * 31 + getMajorVersion()) * 31 +
                getMinorVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayHttpVersion)) {
            return false;
        }

        PlayHttpVersion that = (PlayHttpVersion) o;
        return getMinorVersion() == that.getMinorVersion() &&
                getMajorVersion() == that.getMajorVersion() &&
                getProtocolName().equals(that.getProtocolName());
    }

    public int compareTo(PlayHttpVersion o) {
        int v = getProtocolName().compareTo(o.getProtocolName());
        if (v != 0) {
            return v;
        }

        v = getMajorVersion() - o.getMajorVersion();
        if (v != 0) {
            return v;
        }

        return getMinorVersion() - o.getMinorVersion();
    }
}
