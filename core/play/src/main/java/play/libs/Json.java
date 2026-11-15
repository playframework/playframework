/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Helper functions to handle JsonNode values. */
public class Json {

  /**
   * Gets the JsonMapper used to serialize and deserialize objects to and from JSON values.
   *
   * <p>This can be set to a custom implementation using Json.setObjectMapper.
   *
   * @return the JsonMapper currently being used
   */
  public static JsonMapper mapper() {
    return play.api.libs.json.jackson.JacksonJson$.MODULE$.get().mapper();
  }

  private static String generateJson(Object o, boolean prettyPrint, boolean escapeNonASCII) {
    ObjectWriter writer = mapper().writer();
    if (prettyPrint) {
      writer = writer.with(SerializationFeature.INDENT_OUTPUT);
    }
    if (escapeNonASCII) {
      writer = writer.with(JsonWriteFeature.ESCAPE_NON_ASCII);
    }
    return writer.writeValueAsString(o);
  }

  /**
   * Converts an object to JsonNode.
   *
   * @param data Value to convert in Json.
   * @return the JSON node.
   */
  public static JsonNode toJson(final Object data) {
    try {
      return mapper().valueToTree(data);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts a JsonNode to a Java value
   *
   * @param <A> the type of the return value.
   * @param json Json value to convert.
   * @param clazz Expected Java value type.
   * @return the return value.
   */
  public static <A> A fromJson(JsonNode json, Class<A> clazz) {
    try {
      return mapper().treeToValue(json, clazz);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new empty ObjectNode.
   *
   * @return new empty ObjectNode.
   */
  public static ObjectNode newObject() {
    return mapper().createObjectNode();
  }

  /**
   * Creates a new empty ArrayNode.
   *
   * @return a new empty ArrayNode.
   */
  public static ArrayNode newArray() {
    return mapper().createArrayNode();
  }

  /**
   * Converts a JsonNode to its string representation.
   *
   * @param json the JSON node to convert.
   * @return the string representation.
   */
  public static String stringify(JsonNode json) {
    return generateJson(json, false, false);
  }

  /**
   * Converts a JsonNode to its string representation, escaping non-ascii characters.
   *
   * @param json the JSON node to convert.
   * @return the string representation with escaped non-ascii characters.
   */
  public static String asciiStringify(JsonNode json) {
    return generateJson(json, false, true);
  }

  /**
   * Converts a JsonNode to its string representation.
   *
   * @param json the JSON node to convert.
   * @return the string representation, pretty printed.
   */
  public static String prettyPrint(JsonNode json) {
    return generateJson(json, true, false);
  }

  /**
   * Parses a String representing a json, and return it as a JsonNode.
   *
   * @param src the JSON string.
   * @return the JSON node.
   */
  public static JsonNode parse(String src) {
    try {
      return mapper().readTree(src);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * Parses a InputStream representing a json, and return it as a JsonNode.
   *
   * @param src the JSON input stream.
   * @return the JSON node.
   */
  public static JsonNode parse(java.io.InputStream src) {
    try {
      return mapper().readTree(src);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * Parses a byte array representing a json, and return it as a JsonNode.
   *
   * @param src the JSON input bytes.
   * @return the JSON node.
   */
  public static JsonNode parse(byte[] src) {
    try {
      return mapper().readTree(src);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * Inject the object mapper to use.
   *
   * <p>This is intended to be used when Play starts up. By default, Play will inject its own object
   * mapper here, but this mapper can be overridden either by a custom module.
   *
   * @param mapper the json mapper (or null to reset to the default).
   */
  public static void setObjectMapper(JsonMapper mapper) {
    play.api.libs.json.jackson.JacksonJson$.MODULE$.get().setObjectMapper(mapper);
  }
}
