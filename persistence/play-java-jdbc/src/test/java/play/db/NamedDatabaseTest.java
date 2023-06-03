/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.Map;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import play.ApplicationLoader.Context;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

public class NamedDatabaseTest {

  @Test
  public void bindDatabasesByName() {
    Map<String, Object> config =
        ImmutableMap.of(
            "db.default.driver",
            "org.h2.Driver",
            "db.default.url",
            "jdbc:h2:mem:default",
            "db.other.driver",
            "org.h2.Driver",
            "db.other.url",
            "jdbc:h2:mem:other");
    Injector injector = createInjector(config);
    assertEquals("jdbc:h2:mem:default", injector.getInstance(DefaultComponent.class).db.getUrl());
    assertEquals(
        "jdbc:h2:mem:default", injector.getInstance(NamedDefaultComponent.class).db.getUrl());
    assertEquals("jdbc:h2:mem:other", injector.getInstance(NamedOtherComponent.class).db.getUrl());
  }

  private Injector createInjector(Map<String, Object> config) {
    GuiceApplicationBuilder builder =
        new GuiceApplicationLoader().builder(new Context(Environment.simple(), config));
    return Guice.createInjector(builder.applicationModule());
  }

  @Test
  public void notBindDefaultDatabaseWithoutConfiguration() {
    Map<String, Object> config =
        ImmutableMap.of("db.other.driver", "org.h2.Driver", "db.other.url", "jdbc:h2:mem:other");
    Injector injector = createInjector(config);
    assertEquals("jdbc:h2:mem:other", injector.getInstance(NamedOtherComponent.class).db.getUrl());
    assertThrowsExactly(
        com.google.inject.ConfigurationException.class,
        () -> injector.getInstance(DefaultComponent.class));
  }

  @Test
  public void notBindNamedDefaultDatabaseWithoutConfiguration() {
    Map<String, Object> config =
        ImmutableMap.of("db.other.driver", "org.h2.Driver", "db.other.url", "jdbc:h2:mem:other");
    Injector injector = createInjector(config);
    assertEquals("jdbc:h2:mem:other", injector.getInstance(NamedOtherComponent.class).db.getUrl());
    assertThrowsExactly(
        com.google.inject.ConfigurationException.class,
        () -> injector.getInstance(NamedDefaultComponent.class));
  }

  @Test
  public void allowDefaultDatabaseNameToBeConfigured() {
    Map<String, Object> config =
        ImmutableMap.of(
            "play.db.default",
            "other",
            "db.other.driver",
            "org.h2.Driver",
            "db.other.url",
            "jdbc:h2:mem:other");
    Injector injector = createInjector(config);
    assertEquals("jdbc:h2:mem:other", injector.getInstance(DefaultComponent.class).db.getUrl());
    assertEquals("jdbc:h2:mem:other", injector.getInstance(NamedOtherComponent.class).db.getUrl());

    assertThrowsExactly(
        com.google.inject.ConfigurationException.class,
        () -> injector.getInstance(NamedDefaultComponent.class));
  }

  @Test
  public void allowDbConfigKeyToBeConfigured() {
    Map<String, Object> config =
        ImmutableMap.of(
            "play.db.config",
            "databases",
            "databases.default.driver",
            "org.h2.Driver",
            "databases.default.url",
            "jdbc:h2:mem:default");
    Injector injector = createInjector(config);
    assertEquals("jdbc:h2:mem:default", injector.getInstance(DefaultComponent.class).db.getUrl());
    assertEquals(
        "jdbc:h2:mem:default", injector.getInstance(NamedDefaultComponent.class).db.getUrl());
  }

  public static class DefaultComponent {
    @Inject Database db;
  }

  public static class NamedDefaultComponent {
    @Inject
    @NamedDatabase("default")
    Database db;
  }

  public static class NamedOtherComponent {
    @Inject
    @NamedDatabase("other")
    Database db;
  }
}
