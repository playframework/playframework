/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws;

/**
 * JSON, XML and Multipart Form Data Readables used for Play-WS bodies.
 */
public interface WSBodyReadables extends DefaultBodyReadables, JsonBodyReadables, XMLBodyReadables {
    WSBodyReadables instance = new WSBodyReadables() {};
}
