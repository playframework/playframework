/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import play.test.*;

public class JavaDependencyInjection extends WithApplication {

  @Test
  public void fieldInjection() {
    assertNotNull(app.injector().instanceOf(javaguide.di.field.MyComponent.class));
  }

  @Test
  public void constructorInjection() {
    assertNotNull(app.injector().instanceOf(javaguide.di.constructor.MyComponent.class));
  }

  @Test
  public void singleton() {
    app.injector().instanceOf(CurrentSharePrice.class).set(10);
    assertThat(app.injector().instanceOf(CurrentSharePrice.class).get()).isEqualTo(10);
  }

  @Test
  public void cleanup() {
    app.injector().instanceOf(MessageQueueConnection.class);
    stopPlay();
    assertThat(MessageQueue.stopped).isTrue();
  }

  @Test
  public void implementedBy() {
    assertThat(app.injector().instanceOf(Hello.class).sayHello("world")).isEqualTo("Hello world");
  }
}
