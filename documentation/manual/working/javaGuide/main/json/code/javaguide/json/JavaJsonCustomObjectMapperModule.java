/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.json;

import com.google.inject.AbstractModule;
import com.fasterxml.jackson.databind.ObjectMapper;

// #custom-java-object-mapper2
public class JavaJsonCustomObjectMapperModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ObjectMapper.class).toProvider(JavaJsonCustomObjectMapper.class).asEagerSingleton();
  }
}
// #custom-java-object-mapper2
