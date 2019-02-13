/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Helper functions to handle JsonNode values.
 */
public class Json {
  private static final ObjectMapper defaultObjectMapper = newDefaultMapper();
  private static volatile ObjectMapper objectMapper = null;

  public static ObjectMapper newDefaultMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  /**
   * Gets the ObjectMapper used to serialize and deserialize objects to and from JSON values.
   *
   * This can be set to a custom implementation using Json.setObjectMapper.
   *
   * @return the ObjectMapper currently being used
   */
  public static ObjectMapper mapper() {
    if (objectMapper == null) {
      return defaultObjectMapper;
    } else {
      return objectMapper;
    }
  }

  private static String generateJson(Object o, boolean prettyPrint, boolean escapeNonASCII) {
    try {
      ObjectWriter writer = mapper().writer();
      if (prettyPrint) {
        writer = writer.with(SerializationFeature.INDENT_OUTPUT);
      }
      if (escapeNonASCII) {
        writer = writer.with(Feature.ESCAPE_NON_ASCII);
      }
      return writer.writeValueAsString(o);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    } catch(Exception e) {
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
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new empty ObjectNode.
   * @return new empty ObjectNode.
   */
  public static ObjectNode newObject() {
    return mapper().createObjectNode();
  }

  /**
   * Creates a new empty ArrayNode.
   * @return a new empty ArrayNode.
   */
  public static ArrayNode newArray() {
    return mapper().createArrayNode();
  }

  /**
   * Converts a JsonNode to its string representation.
   * @param json    the JSON node to convert.
   * @return the string representation.
   */
  public static String stringify(JsonNode json) {
    return generateJson(json, false, false);
  }

  /**
   * Converts a JsonNode to its string representation, escaping non-ascii characters.
   * @param json    the JSON node to convert.
   * @return the string representation with escaped non-ascii characters.
   */
  public static String asciiStringify(JsonNode json) {
    return generateJson(json, false, true);
  }

  /**
   * Converts a JsonNode to its string representation.
   * @param json    the JSON node to convert.
   * @return the string representation, pretty printed.
   */
  public static String prettyPrint(JsonNode json) {
    return generateJson(json, true, false);
  }

  /**
   * Parses a String representing a json, and return it as a JsonNode.
   * @param src    the JSON string.
   * @return the JSON node.
   */
  public static JsonNode parse(String src) {
    try {
      return mapper().readTree(src);
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * Parses a InputStream representing a json, and return it as a JsonNode.
   * @param src    the JSON input stream.
   * @return the JSON node.
   */
  public static JsonNode parse(java.io.InputStream src) {
    try {
      return mapper().readTree(src);
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * Parses a byte array representing a json, and return it as a JsonNode.
   * @param src    the JSON input bytes.
   * @return the JSON node.
   */
  public static JsonNode parse(byte[] src) {
    try {
      return mapper().readTree(src);
    } catch(Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * Inject the object mapper to use.
   *
   * This is intended to be used when Play starts up.  By default, Play will inject its own object mapper here,
   * but this mapper can be overridden either by a custom module.
   * @param mapper    the object mapper.
   */
  public static void setObjectMapper(ObjectMapper mapper) {
    objectMapper = mapper;
  }

}
