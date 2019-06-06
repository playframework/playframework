/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.json;

import play.Application;
import play.ApplicationLoader;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

import play.libs.Json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

// #custom-java-object-mapper
public class JavaJsonCustomObjectMapper {

  JavaJsonCustomObjectMapper() {
    ObjectMapper mapper =
        Json.newDefaultMapper()
            // enable features and customize the object mapper here ...
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    // etc.
    Json.setObjectMapper(mapper);
  }
}
// #custom-java-object-mapper
