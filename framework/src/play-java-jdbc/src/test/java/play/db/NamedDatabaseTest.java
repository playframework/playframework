/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.db;

import java.util.Map;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.Test;

import play.api.inject.Injector;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class NamedDatabaseTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void bindDatabasesByName() {
        Map<String, String> config = ImmutableMap.of(
            "db.default.driver", "org.h2.Driver",
            "db.default.url", "jdbc:h2:mem:default",
            "db.other.driver", "org.h2.Driver",
            "db.other.url", "jdbc:h2:mem:other"
        );
        running(fakeApplication(config), new Runnable() {
            public void run() {
                Injector injector = play.api.Play.current().injector();
                assertThat(injector.instanceOf(DefaultComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:default"));
                assertThat(injector.instanceOf(NamedDefaultComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:default"));
                assertThat(injector.instanceOf(NamedOtherComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:other"));
            }
        });
    }

    @Test
    public void notBindDefaultDatabaseWithoutConfiguration() {
        Map<String, String> config = ImmutableMap.of(
            "db.other.driver", "org.h2.Driver",
            "db.other.url", "jdbc:h2:mem:other"
        );
        running(fakeApplication(config), new Runnable() {
            public void run() {
                Injector injector = play.api.Play.current().injector();
                assertThat(injector.instanceOf(NamedOtherComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:other"));
                exception.expect(com.google.inject.ConfigurationException.class);
                injector.instanceOf(DefaultComponent.class);
            }
        });
    }

    @Test
    public void notBindNamedDefaultDatabaseWithoutConfiguration() {
        Map<String, String> config = ImmutableMap.of(
            "db.other.driver", "org.h2.Driver",
            "db.other.url", "jdbc:h2:mem:other"
        );
        running(fakeApplication(config), new Runnable() {
            public void run() {
                Injector injector = play.api.Play.current().injector();
                assertThat(injector.instanceOf(NamedOtherComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:other"));
                exception.expect(com.google.inject.ConfigurationException.class);
                injector.instanceOf(NamedDefaultComponent.class);
            }
        });
    }

    @Test
    public void allowDefaultDatabaseNameToBeConfigured() {
        Map<String, String> config = ImmutableMap.of(
            "play.db.default", "other",
            "db.other.driver", "org.h2.Driver",
            "db.other.url", "jdbc:h2:mem:other"
        );
        running(fakeApplication(config), new Runnable() {
            public void run() {
                Injector injector = play.api.Play.current().injector();
                assertThat(injector.instanceOf(DefaultComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:other"));
                assertThat(injector.instanceOf(NamedOtherComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:other"));
                exception.expect(com.google.inject.ConfigurationException.class);
                injector.instanceOf(NamedDefaultComponent.class);
            }
        });
    }

    @Test
    public void allowDbConfigKeyToBeConfigured() {
        Map<String, String> config = ImmutableMap.of(
            "play.db.config", "databases",
            "databases.default.driver", "org.h2.Driver",
            "databases.default.url", "jdbc:h2:mem:default"
        );
        running(fakeApplication(config), new Runnable() {
            public void run() {
                Injector injector = play.api.Play.current().injector();
                assertThat(injector.instanceOf(DefaultComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:default"));
                assertThat(injector.instanceOf(NamedDefaultComponent.class).db.getUrl(), equalTo("jdbc:h2:mem:default"));
            }
        });
    }

    public static class DefaultComponent {
        @Inject Database db;
    }

    public static class NamedDefaultComponent {
        @Inject @NamedDatabase("default") Database db;
    }

    public static class NamedOtherComponent {
        @Inject @NamedDatabase("other") Database db;
    }

}
