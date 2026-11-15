/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

// refs https://github.com/lagom/lagom/issues/3241
@SuppressWarnings("WeakerAccess")
public class ChildDeserializer extends StdDeserializer<Child> {

  public ChildDeserializer() {
    this(Child.class);
  }

  public ChildDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public Child deserialize(JsonParser jp, DeserializationContext ctxt) {
    JsonNode node = jp.readValueAsTree();
    String updatedBy = node.get("updatedBy").asText();
    Long updatedAt = node.get("updatedAt").asLong();

    return new Child(updatedAt, updatedBy);
  }
}
