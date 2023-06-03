/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static play.inject.Bindings.bind;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import play.Application;
import play.ApplicationLoader;
import play.Environment;

public class GuiceApplicationLoaderTest {

  private ApplicationLoader.Context fakeContext() {
    return ApplicationLoader.create(Environment.simple());
  }

  @Test
  public void additionalModulesAndBindings() {
    GuiceApplicationBuilder builder =
        new GuiceApplicationBuilder().bindings(new AModule()).bindings(bind(B.class).to(B1.class));
    ApplicationLoader loader = new GuiceApplicationLoader(builder);
    Application app = loader.load(fakeContext());

    assertInstanceOf(A1.class, app.injector().instanceOf(A.class));
    assertInstanceOf(B1.class, app.injector().instanceOf(B.class));
  }

  @Test
  public void extendLoaderAndSetConfiguration() {
    ApplicationLoader loader =
        new GuiceApplicationLoader() {
          @Override
          public GuiceApplicationBuilder builder(Context context) {
            Config extra = ConfigFactory.parseString("a = 1");
            return initialBuilder
                .in(context.environment())
                .loadConfig(extra.withFallback(context.initialConfig()))
                .overrides(overrides(context));
          }
        };
    Application app = loader.load(fakeContext());

    assertEquals(1, app.config().getInt("a"));
  }

  @Test
  public void usingAdditionalConfiguration() {
    Properties properties = new Properties();
    properties.setProperty("play.http.context", "/tests");

    Config config =
        ConfigFactory.parseProperties(properties).withFallback(ConfigFactory.defaultReference());

    GuiceApplicationBuilder builder = new GuiceApplicationBuilder();
    ApplicationLoader loader = new GuiceApplicationLoader(builder);
    ApplicationLoader.Context context =
        ApplicationLoader.create(Environment.simple()).withConfig(config);
    Application app = loader.load(context);

    assertEquals("/tests", app.asScala().httpConfiguration().context());
  }

  public interface A {}

  public static class A1 implements A {}

  public static class AModule extends com.google.inject.AbstractModule {
    public void configure() {
      bind(A.class).to(A1.class);
    }
  }

  public interface B {}

  public static class B1 implements B {}
}
