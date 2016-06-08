/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.inject.guice;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import play.Application;
import play.ApplicationLoader;
import play.Environment;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static play.inject.Bindings.bind;

public class GuiceApplicationLoaderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ApplicationLoader.Context fakeContext() {
        return ApplicationLoader.Context.create(Environment.simple());
    }

    @Test
    public void additionalModulesAndBindings() {
        GuiceApplicationBuilder builder = new GuiceApplicationBuilder()
            .bindings(new AModule())
            .bindings(bind(B.class).to(B1.class));
        ApplicationLoader loader = new GuiceApplicationLoader(builder);
        Application app = loader.load(fakeContext());

        assertThat(app.injector().instanceOf(A.class), instanceOf(A1.class));
        assertThat(app.injector().instanceOf(B.class), instanceOf(B1.class));
    }

    @Test
    public void extendLoaderAndSetConfiguration() {
        ApplicationLoader loader = new GuiceApplicationLoader() {
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

        assertThat(app.configuration().getInt("a"), is(1));
    }

    public static interface A {}
    public static class A1 implements A {}

    public static class AModule extends com.google.inject.AbstractModule {
        public void configure() {
            bind(A.class).to(A1.class);
        }
    }

    public static interface B {}
    public static class B1 implements B {}

}
