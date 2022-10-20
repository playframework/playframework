/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;

// #custom-java-object-mapper2
public class JavaJsonCustomObjectMapperModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ObjectMapper.class).toProvider(JavaJsonCustomObjectMapper.class).asEagerSingleton();
  }
}
// #custom-java-object-mapper2
