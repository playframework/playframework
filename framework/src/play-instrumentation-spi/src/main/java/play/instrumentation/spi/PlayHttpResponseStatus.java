/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.instrumentation.spi;

public class PlayHttpResponseStatus implements Comparable<PlayHttpResponseStatus> {
    public static final PlayHttpResponseStatus CONTINUE = new PlayHttpResponseStatus(100, "Continue");
    public static final PlayHttpResponseStatus SWITCHING_PROTOCOLS = new PlayHttpResponseStatus(101, "Switching Protocols");
    public static final PlayHttpResponseStatus PROCESSING = new PlayHttpResponseStatus(102, "Processing");
    public static final PlayHttpResponseStatus OK = new PlayHttpResponseStatus(200, "OK");
    public static final PlayHttpResponseStatus CREATED = new PlayHttpResponseStatus(201, "Created");
    public static final PlayHttpResponseStatus ACCEPTED = new PlayHttpResponseStatus(202, "Accepted");
    public static final PlayHttpResponseStatus NON_AUTHORITATIVE_INFORMATION =
            new PlayHttpResponseStatus(203, "Non-Authoritative Information");
    public static final PlayHttpResponseStatus NO_CONTENT = new PlayHttpResponseStatus(204, "No Content");
    public static final PlayHttpResponseStatus RESET_CONTENT = new PlayHttpResponseStatus(205, "Reset Content");
    public static final PlayHttpResponseStatus PARTIAL_CONTENT = new PlayHttpResponseStatus(206, "Partial Content");
    public static final PlayHttpResponseStatus MULTI_STATUS = new PlayHttpResponseStatus(207, "Multi-Status");
    public static final PlayHttpResponseStatus MULTIPLE_CHOICES = new PlayHttpResponseStatus(300, "Multiple Choices");
    public static final PlayHttpResponseStatus MOVED_PERMANENTLY = new PlayHttpResponseStatus(301, "Moved Permanently");
    public static final PlayHttpResponseStatus FOUND = new PlayHttpResponseStatus(302, "Found");
    public static final PlayHttpResponseStatus SEE_OTHER = new PlayHttpResponseStatus(303, "See Other");
    public static final PlayHttpResponseStatus NOT_MODIFIED = new PlayHttpResponseStatus(304, "Not Modified");
    public static final PlayHttpResponseStatus USE_PROXY = new PlayHttpResponseStatus(305, "Use Proxy");
    public static final PlayHttpResponseStatus TEMPORARY_REDIRECT = new PlayHttpResponseStatus(307, "Temporary Redirect");
    public static final PlayHttpResponseStatus BAD_REQUEST = new PlayHttpResponseStatus(400, "Bad Request");
    public static final PlayHttpResponseStatus UNAUTHORIZED = new PlayHttpResponseStatus(401, "Unauthorized");
    public static final PlayHttpResponseStatus PAYMENT_REQUIRED = new PlayHttpResponseStatus(402, "Payment Required");
    public static final PlayHttpResponseStatus FORBIDDEN = new PlayHttpResponseStatus(403, "Forbidden");
    public static final PlayHttpResponseStatus NOT_FOUND = new PlayHttpResponseStatus(404, "Not Found");
    public static final PlayHttpResponseStatus METHOD_NOT_ALLOWED = new PlayHttpResponseStatus(405, "Method Not Allowed");
    public static final PlayHttpResponseStatus NOT_ACCEPTABLE = new PlayHttpResponseStatus(406, "Not Acceptable");
    public static final PlayHttpResponseStatus PROXY_AUTHENTICATION_REQUIRED =
            new PlayHttpResponseStatus(407, "Proxy Authentication Required");
    public static final PlayHttpResponseStatus REQUEST_TIMEOUT = new PlayHttpResponseStatus(408, "Request Timeout");
    public static final PlayHttpResponseStatus CONFLICT = new PlayHttpResponseStatus(409, "Conflict");
    public static final PlayHttpResponseStatus GONE = new PlayHttpResponseStatus(410, "Gone");
    public static final PlayHttpResponseStatus LENGTH_REQUIRED = new PlayHttpResponseStatus(411, "Length Required");
    public static final PlayHttpResponseStatus PRECONDITION_FAILED = new PlayHttpResponseStatus(412, "Precondition Failed");
    public static final PlayHttpResponseStatus REQUEST_ENTITY_TOO_LARGE =
            new PlayHttpResponseStatus(413, "Request Entity Too Large");
    public static final PlayHttpResponseStatus REQUEST_URI_TOO_LONG = new PlayHttpResponseStatus(414, "Request-URI Too Long");
    public static final PlayHttpResponseStatus UNSUPPORTED_MEDIA_TYPE =
            new PlayHttpResponseStatus(415, "Unsupported Media Type");
    public static final PlayHttpResponseStatus REQUESTED_RANGE_NOT_SATISFIABLE =
            new PlayHttpResponseStatus(416, "Requested Range Not Satisfiable");
    public static final PlayHttpResponseStatus EXPECTATION_FAILED = new PlayHttpResponseStatus(417, "Expectation Failed");
    public static final PlayHttpResponseStatus UNPROCESSABLE_ENTITY = new PlayHttpResponseStatus(422, "Unprocessable Entity");
    public static final PlayHttpResponseStatus LOCKED = new PlayHttpResponseStatus(423, "Locked");
    public static final PlayHttpResponseStatus FAILED_DEPENDENCY = new PlayHttpResponseStatus(424, "Failed Dependency");
    public static final PlayHttpResponseStatus UNORDERED_COLLECTION = new PlayHttpResponseStatus(425, "Unordered Collection");
    public static final PlayHttpResponseStatus UPGRADE_REQUIRED = new PlayHttpResponseStatus(426, "Upgrade Required");
    public static final PlayHttpResponseStatus REQUEST_HEADER_FIELDS_TOO_LARGE =
            new PlayHttpResponseStatus(431, "Request Header Fields Too Large");
    public static final PlayHttpResponseStatus INTERNAL_SERVER_ERROR =
            new PlayHttpResponseStatus(500, "Internal Server Error");
    public static final PlayHttpResponseStatus NOT_IMPLEMENTED = new PlayHttpResponseStatus(501, "Not Implemented");
    public static final PlayHttpResponseStatus BAD_GATEWAY = new PlayHttpResponseStatus(502, "Bad Gateway");
    public static final PlayHttpResponseStatus SERVICE_UNAVAILABLE = new PlayHttpResponseStatus(503, "Service Unavailable");
    public static final PlayHttpResponseStatus GATEWAY_TIMEOUT = new PlayHttpResponseStatus(504, "Gateway Timeout");
    public static final PlayHttpResponseStatus HTTP_VERSION_NOT_SUPPORTED =
            new PlayHttpResponseStatus(505, "HTTP Version Not Supported");
    public static final PlayHttpResponseStatus VARIANT_ALSO_NEGOTIATES =
            new PlayHttpResponseStatus(506, "Variant Also Negotiates");
    public static final PlayHttpResponseStatus INSUFFICIENT_STORAGE = new PlayHttpResponseStatus(507, "Insufficient Storage");
    public static final PlayHttpResponseStatus NOT_EXTENDED = new PlayHttpResponseStatus(510, "Not Extended");

    public static PlayHttpResponseStatus valueOf(int code) {
        switch (code) {
            case 100:
                return CONTINUE;
            case 101:
                return SWITCHING_PROTOCOLS;
            case 102:
                return PROCESSING;
            case 200:
                return OK;
            case 201:
                return CREATED;
            case 202:
                return ACCEPTED;
            case 203:
                return NON_AUTHORITATIVE_INFORMATION;
            case 204:
                return NO_CONTENT;
            case 205:
                return RESET_CONTENT;
            case 206:
                return PARTIAL_CONTENT;
            case 207:
                return MULTI_STATUS;
            case 300:
                return MULTIPLE_CHOICES;
            case 301:
                return MOVED_PERMANENTLY;
            case 302:
                return FOUND;
            case 303:
                return SEE_OTHER;
            case 304:
                return NOT_MODIFIED;
            case 305:
                return USE_PROXY;
            case 307:
                return TEMPORARY_REDIRECT;
            case 400:
                return BAD_REQUEST;
            case 401:
                return UNAUTHORIZED;
            case 402:
                return PAYMENT_REQUIRED;
            case 403:
                return FORBIDDEN;
            case 404:
                return NOT_FOUND;
            case 405:
                return METHOD_NOT_ALLOWED;
            case 406:
                return NOT_ACCEPTABLE;
            case 407:
                return PROXY_AUTHENTICATION_REQUIRED;
            case 408:
                return REQUEST_TIMEOUT;
            case 409:
                return CONFLICT;
            case 410:
                return GONE;
            case 411:
                return LENGTH_REQUIRED;
            case 412:
                return PRECONDITION_FAILED;
            case 413:
                return REQUEST_ENTITY_TOO_LARGE;
            case 414:
                return REQUEST_URI_TOO_LONG;
            case 415:
                return UNSUPPORTED_MEDIA_TYPE;
            case 416:
                return REQUESTED_RANGE_NOT_SATISFIABLE;
            case 417:
                return EXPECTATION_FAILED;
            case 422:
                return UNPROCESSABLE_ENTITY;
            case 423:
                return LOCKED;
            case 424:
                return FAILED_DEPENDENCY;
            case 425:
                return UNORDERED_COLLECTION;
            case 426:
                return UPGRADE_REQUIRED;
            case 500:
                return INTERNAL_SERVER_ERROR;
            case 501:
                return NOT_IMPLEMENTED;
            case 502:
                return BAD_GATEWAY;
            case 503:
                return SERVICE_UNAVAILABLE;
            case 504:
                return GATEWAY_TIMEOUT;
            case 505:
                return HTTP_VERSION_NOT_SUPPORTED;
            case 506:
                return VARIANT_ALSO_NEGOTIATES;
            case 507:
                return INSUFFICIENT_STORAGE;
            case 510:
                return NOT_EXTENDED;
        }

        final String reasonPhrase;

        if (code < 100) {
            reasonPhrase = "Unknown Status";
        } else if (code < 200) {
            reasonPhrase = "Informational";
        } else if (code < 300) {
            reasonPhrase = "Successful";
        } else if (code < 400) {
            reasonPhrase = "Redirection";
        } else if (code < 500) {
            reasonPhrase = "Client Error";
        } else if (code < 600) {
            reasonPhrase = "Server Error";
        } else {
            reasonPhrase = "Unknown Status";
        }

        return new PlayHttpResponseStatus(code, reasonPhrase + " (" + code + ')');
    }

    private final int code;

    private final String reasonPhrase;

    public PlayHttpResponseStatus(int code, String reasonPhrase) {
        if (code < 0) {
            throw new IllegalArgumentException(
                    "code: " + code + " (expected: 0+)");
        }

        if (reasonPhrase == null) {
            throw new NullPointerException("reasonPhrase");
        }

        for (int i = 0; i < reasonPhrase.length(); i ++) {
            char c = reasonPhrase.charAt(i);
            // Check prohibited characters.
            switch (c) {
                case '\n': case '\r':
                    throw new IllegalArgumentException(
                            "reasonPhrase contains one of the following prohibited characters: " +
                                    "\\r\\n: " + reasonPhrase);
            }
        }

        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    public int getCode() {
        return code;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public int hashCode() {
        return getCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayHttpResponseStatus)) {
            return false;
        }

        return getCode() == ((PlayHttpResponseStatus) o).getCode();
    }

    public int compareTo(PlayHttpResponseStatus o) {
        return getCode() - o.getCode();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(reasonPhrase.length() + 5);
        buf.append(code);
        buf.append(' ');
        buf.append(reasonPhrase);
        return buf.toString();
    }
}
