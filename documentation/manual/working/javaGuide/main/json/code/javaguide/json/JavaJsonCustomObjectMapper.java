/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.json;

import play.Application;
import play.ApplicationLoader;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

import play.libs.Json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class JavaJsonCustomObjectMapper {

    //#custom-apploader-object-mapper
    public class ObjectMapperApplicationLoader extends GuiceApplicationLoader {
        @Override
        public GuiceApplicationBuilder builder(Context context) {
            ObjectMapper mapper = Json.newDefaultMapper()
                    // enable features and customize the object mapper here ...
                    .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    // etc.
            Json.setObjectMapper(mapper);
            return super.builder(context);
        }
    }
    //#custom-apploader-object-mapper
}
