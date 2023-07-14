/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.test.junit5;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;
import play.test.junit5.ApplicationExtension;

class InjectionTest {

  static Module testModule =
      new AbstractModule() {
        @Override
        public void configure() {
          // Install custom test binding here
        }
      };

  // #test-injection
  @RegisterExtension
  static ApplicationExtension applicationExtension = new ApplicationExtension(createApplication());

  static Application createApplication() {
    GuiceApplicationBuilder builder =
        new GuiceApplicationLoader()
            .builder(new Context(Environment.simple()))
            .overrides(testModule);

    return Guice.createInjector(builder.applicationModule()).getInstance(Application.class);
  }

  @Test
  void testApplication() {
    Application application = applicationExtension.getApplication();
    assertNotNull(application);
  }
}
