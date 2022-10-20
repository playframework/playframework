/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Provider;
import play.libs.Json;

// #custom-java-object-mapper
public class JavaJsonCustomObjectMapper implements Provider<ObjectMapper> {

  @Override
  public ObjectMapper get() {
    ObjectMapper mapper =
        new ObjectMapper()
            // enable features and customize the object mapper here ...
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Needs to set to Json helper
    Json.setObjectMapper(mapper);

    return mapper;
  }
}
// #custom-java-object-mapper
