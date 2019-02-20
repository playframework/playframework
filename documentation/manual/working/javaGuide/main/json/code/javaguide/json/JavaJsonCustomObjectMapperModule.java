/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.json;

import com.google.inject.AbstractModule;

// #custom-java-object-mapper2
public class JavaJsonCustomObjectMapperModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(JavaJsonCustomObjectMapper.class).asEagerSingleton();
  }
}
// #custom-java-object-mapper2
