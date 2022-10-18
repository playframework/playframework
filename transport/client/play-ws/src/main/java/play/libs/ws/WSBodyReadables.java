/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws;

/** JSON, XML and Multipart Form Data Readables used for Play-WS bodies. */
public interface WSBodyReadables extends DefaultBodyReadables, JsonBodyReadables, XMLBodyReadables {
  WSBodyReadables instance = new WSBodyReadables() {};
}
