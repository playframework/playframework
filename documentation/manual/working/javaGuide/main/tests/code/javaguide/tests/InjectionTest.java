/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.tests;

import static org.junit.Assert.*;
import static play.test.Helpers.*;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;
import play.test.Helpers;

public class InjectionTest {

  // #test-injection
  @Inject Application application;

  @Before
  public void setup() {
    Module testModule =
        new AbstractModule() {
          @Override
          public void configure() {
            // Install custom test binding here
          }
        };

    GuiceApplicationBuilder builder =
        new GuiceApplicationLoader()
            .builder(new Context(Environment.simple()))
            .overrides(testModule);
    Guice.createInjector(builder.applicationModule()).injectMembers(this);

    Helpers.start(application);
  }

  @After
  public void teardown() {
    Helpers.stop(application);
  }
  // #test-injection

}
