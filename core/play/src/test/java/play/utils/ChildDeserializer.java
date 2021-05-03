/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

// refs https://github.com/lagom/lagom/issues/3241
@SuppressWarnings("WeakerAccess")
public class ChildDeserializer extends StdDeserializer<Child> {

  public ChildDeserializer() {
    this(null);
  }

  public ChildDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Child deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    JsonNode node = jp.readValueAsTree();
    String updatedBy = node.get("updatedBy").asText();
    Long updatedAt = node.get("updatedAt").asLong();

    return new Child(updatedAt, updatedBy);
  }
}
