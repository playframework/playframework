/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

import static org.junit.jupiter.api.Assertions.*;
import static play.test.Helpers.*;

import org.junit.jupiter.api.Test;
import play.Application;
import play.test.*;
import play.test.junit5.ApplicationExtension;

public class JavaDependencyInjection {

  static ApplicationExtension appExtension = new ApplicationExtension(fakeApplication());
  static Application app = appExtension.getApplication();

  @Test
  void fieldInjection() {
    assertNotNull(app.injector().instanceOf(javaguide.di.field.MyComponent.class));
  }

  @Test
  void constructorInjection() {
    assertNotNull(app.injector().instanceOf(javaguide.di.constructor.MyComponent.class));
  }

  @Test
  void singleton() {
    app.injector().instanceOf(CurrentSharePrice.class).set(10);
    assertEquals(10, app.injector().instanceOf(CurrentSharePrice.class).get());
  }

  @Test
  void cleanup() {
    app.injector().instanceOf(MessageQueueConnection.class);
    stop(app);
    assertTrue(MessageQueue.stopped);
  }

  @Test
  void implementedBy() {
    assertEquals("Hello world", app.injector().instanceOf(Hello.class).sayHello("world"));
  }
}
