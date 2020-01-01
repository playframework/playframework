/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.json;

import play.Application;
import play.ApplicationLoader;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

import play.libs.Json;
import javax.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

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
