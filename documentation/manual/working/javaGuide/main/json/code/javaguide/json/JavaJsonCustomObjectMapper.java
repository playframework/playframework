/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.json;

import jakarta.inject.Provider;
import play.libs.Json;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

// #custom-java-object-mapper
public class JavaJsonCustomObjectMapper implements Provider<JsonMapper> {

  @Override
  public JsonMapper get() {
    JsonMapper mapper =
        JsonMapper.builder()
            // enable features and customize the object mapper here ...
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    // Needs to set to Json helper
    Json.setObjectMapper(mapper);

    return mapper;
  }
}
// #custom-java-object-mapper
