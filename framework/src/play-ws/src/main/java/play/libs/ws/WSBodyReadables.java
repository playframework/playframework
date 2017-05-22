/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws;

/**
 *
 */
public interface WSBodyReadables extends DefaultBodyReadables, JsonBodyReadables, XMLBodyReadables {
    WSBodyReadables instance = new WSBodyReadables() {};
}
