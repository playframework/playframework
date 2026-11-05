/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.inject.guice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static play.inject.Bindings.bind;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Key;
import com.google.inject.ProvidedBy;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.inject.Binding;
import play.inject.Injector;
import play.inject.Module;

public class NoScopeTest {
  private static final Mode[] MODES = {Mode.DEV, Mode.PROD};

  @Test
  public void preservesSingletonWithoutNoScope() {
    for (Mode mode : MODES) {
      Injector injector =
          new GuiceInjectorBuilder().in(mode).bindings(new SingletonModule()).injector();
      assertThat(injector.instanceOf(Service.class)).isSameAs(injector.instanceOf(Service.class));
    }
  }

  @Test
  public void constructsFreshInstancesThroughAPlayModule() {
    for (Mode mode : MODES) {
      Injector injector =
          new GuiceInjectorBuilder().in(mode).bindings(new NoScopeModule()).injector();
      Service first = injector.instanceOf(Service.class);
      SingletonService implementation = injector.instanceOf(SingletonService.class);
      assertThat(first).isNotSameAs(injector.instanceOf(Service.class));
      assertThat(first).isNotSameAs(implementation);
      assertThat(implementation).isSameAs(injector.instanceOf(SingletonService.class));
    }
  }

  @Test
  public void overridesGuicesSingletonAnnotation() {
    for (Mode mode : MODES) {
      Injector injector =
          new GuiceInjectorBuilder()
              .in(mode)
              .bindings(bind(Service.class).to(GuiceSingletonService.class).in(NoScope.class))
              .injector();
      assertThat(injector.instanceOf(Service.class))
          .isNotSameAs(injector.instanceOf(Service.class));
      assertThat(injector.instanceOf(GuiceSingletonService.class))
          .isSameAs(injector.instanceOf(GuiceSingletonService.class));
    }
  }

  @Test
  public void supportsQualifiedConstructionBindings() {
    for (Mode mode : MODES) {
      com.google.inject.Injector injector =
          new GuiceInjectorBuilder()
              .in(mode)
              .bindings(
                  bind(Service.class)
                      .qualifiedWith("fresh")
                      .to(SingletonService.class)
                      .in(NoScope.class),
                  bind(Service.class).to(SingletonService.class))
              .injector()
              .instanceOf(com.google.inject.Injector.class);
      Key<Service> fresh = Key.get(Service.class, Names.named("fresh"));
      assertThat(injector.getInstance(fresh)).isNotSameAs(injector.getInstance(fresh));
      assertThat(injector.getInstance(Service.class))
          .isSameAs(injector.getInstance(SingletonService.class));
    }
  }

  @Test
  public void supportsSelfBindings() {
    for (Mode mode : MODES) {
      Injector injector =
          new GuiceInjectorBuilder()
              .in(mode)
              .bindings(
                  bind(Service.class).to(SingletonService.class),
                  bind(SingletonService.class).toSelf().in(NoScope.class))
              .injector();
      assertThat(injector.instanceOf(Service.class))
          .isNotSameAs(injector.instanceOf(Service.class));
      assertThat(injector.instanceOf(SingletonService.class))
          .isNotSameAs(injector.instanceOf(SingletonService.class));
    }
  }

  @Test
  public void preservesSingletonScopesOnOtherBindings() {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(
                bind(Service.class).to(SingletonService.class).in(Singleton.class),
                bind(SingletonService.class).toSelf().in(NoScope.class))
            .injector();
    assertThat(injector.instanceOf(Service.class)).isSameAs(injector.instanceOf(Service.class));
    assertThat(injector.instanceOf(SingletonService.class))
        .isNotSameAs(injector.instanceOf(SingletonService.class));
  }

  @Test
  public void supportsProvidedBySelfBindings() {
    for (Mode mode : MODES) {
      Injector scoped =
          new GuiceInjectorBuilder()
              .in(mode)
              .bindings(bind(ProvidedService.class).toSelf())
              .injector();
      assertThat(scoped.instanceOf(ProvidedService.class))
          .isSameAs(scoped.instanceOf(ProvidedService.class));
      Injector unscoped =
          new GuiceInjectorBuilder()
              .in(mode)
              .bindings(bind(ProvidedService.class).toSelf().in(NoScope.class))
              .injector();
      assertThat(unscoped.instanceOf(ProvidedService.class))
          .isNotSameAs(unscoped.instanceOf(ProvidedService.class));
    }
  }

  @Test
  public void leavesReuseToProvidersAndAliasTargets() {
    SingletonService cached = new SingletonService();
    com.google.inject.Injector injector =
        new GuiceInjectorBuilder()
            .bindings(
                bind(Service.class)
                    .qualifiedWith("provider-instance")
                    .to((Provider<Service>) () -> cached)
                    .in(NoScope.class),
                bind(Service.class)
                    .qualifiedWith("provider-class")
                    .toProvider(CachingProvider.class)
                    .in(NoScope.class),
                bind(Service.class)
                    .qualifiedWith("fresh-provider")
                    .toProvider(FreshProvider.class)
                    .in(NoScope.class),
                bind(Service.class)
                    .qualifiedWith("alias")
                    .to(bind(SingletonService.class))
                    .in(NoScope.class))
            .injector()
            .instanceOf(com.google.inject.Injector.class);
    Key<Service> instance = Key.get(Service.class, Names.named("provider-instance"));
    Key<Service> provider = Key.get(Service.class, Names.named("provider-class"));
    Key<Service> fresh = Key.get(Service.class, Names.named("fresh-provider"));
    Key<Service> alias = Key.get(Service.class, Names.named("alias"));
    assertThat(injector.getInstance(instance)).isSameAs(cached);
    assertThat(injector.getInstance(provider)).isSameAs(injector.getInstance(provider));
    assertThat(injector.getInstance(fresh)).isNotSameAs(injector.getInstance(fresh));
    assertThat(injector.getInstance(alias)).isSameAs(injector.getInstance(SingletonService.class));
  }

  @Test
  public void injectsConstructorFieldsAndMethodsAndPreservesInterceptors() {
    Injector injector =
        new GuiceInjectorBuilder()
            .bindings(bind(Service.class).to(InjectedService.class).in(NoScope.class))
            .bindings(
                new AbstractModule() {
                  @Override
                  protected void configure() {
                    bindInterceptor(
                        Matchers.subclassesOf(InjectedService.class),
                        Matchers.any(),
                        invocation ->
                            invocation.getMethod().getName().equals("message")
                                ? "intercepted " + invocation.proceed()
                                : invocation.proceed());
                  }
                })
            .injector();
    InjectedService first = (InjectedService) injector.instanceOf(Service.class);
    Dependency dependency = injector.instanceOf(Dependency.class);
    assertThat(first).isNotSameAs(injector.instanceOf(Service.class));
    assertThat(first.constructorDependency).isSameAs(dependency);
    assertThat(first.fieldDependency).isSameAs(dependency);
    assertThat(first.methodDependency).isSameAs(dependency);
    assertThat(first.message()).isEqualTo("intercepted hello");
  }

  @Test
  public void reportsAnUnusableConstructorWithoutFollowingProvidedBy() {
    CreationException error =
        assertThrows(
            CreationException.class,
            () ->
                new GuiceInjectorBuilder()
                    .bindings(bind(Service.class).to(ProvidedService.class).in(NoScope.class))
                    .injector());
    assertThat(error.getErrorMessages())
        .anySatisfy(
            message ->
                assertThat(message.getMessage())
                    .contains("NoScope binding to " + ProvidedService.class.getName())
                    .contains("Use a provider binding"));
    assertThat(error).hasMessageNotContaining("MissingImplementation");
  }

  @Test
  public void bypassesImplementationOverridesAndAcceptsInterfaceOverrides() {
    SingletonService replacement = new SingletonService();
    GuiceInjectorBuilder builder =
        new GuiceInjectorBuilder()
            .bindings(bind(Service.class).to(SingletonService.class).in(NoScope.class));
    Injector implementationOverride =
        builder.overrides(bind(SingletonService.class).toInstance(replacement)).injector();
    assertThat(implementationOverride.instanceOf(SingletonService.class)).isSameAs(replacement);
    assertThat(implementationOverride.instanceOf(Service.class)).isNotSameAs(replacement);
    assertThat(
            builder
                .overrides(bind(Service.class).toInstance(replacement))
                .injector()
                .instanceOf(Service.class))
        .isSameAs(replacement);
  }

  @Test
  public void requiresAnOverrideForAnExistingLinkedBinding() {
    GuiceInjectorBuilder library = new GuiceInjectorBuilder().bindings(new SingletonModule());
    CreationException error =
        assertThrows(
            CreationException.class, () -> library.bindings(new NoScopeModule()).injector());
    assertThat(error).hasMessageContaining("BindingAlreadySet");
    Injector overridden = library.overrides(new NoScopeModule()).injector();
    assertThat(overridden.instanceOf(Service.class))
        .isNotSameAs(overridden.instanceOf(Service.class));
  }

  @Test
  public void acceptsIdenticalConstructorBindingsFromTwoModules() {
    Injector injector =
        new GuiceInjectorBuilder().bindings(new NoScopeModule(), new NoScopeModule()).injector();
    assertThat(injector.instanceOf(Service.class)).isNotSameAs(injector.instanceOf(Service.class));
  }

  public interface Service {}

  @Singleton
  public static class SingletonService implements Service {}

  @com.google.inject.Singleton
  public static class GuiceSingletonService implements Service {}

  public static class SingletonModule extends Module {
    @Override
    public List<Binding<?>> bindings(Environment environment, Config config) {
      return Collections.singletonList(bindClass(Service.class).to(SingletonService.class));
    }
  }

  public static class NoScopeModule extends Module {
    @Override
    public List<Binding<?>> bindings(Environment environment, Config config) {
      return Collections.singletonList(
          bindClass(Service.class).to(SingletonService.class).in(NoScope.class));
    }
  }

  @Singleton
  public static class Dependency {
    @Inject
    public Dependency() {}
  }

  @Singleton
  public static class InjectedService implements Service {
    final Dependency constructorDependency;
    @Inject Dependency fieldDependency;
    Dependency methodDependency;

    @Inject
    public InjectedService(Dependency dependency) {
      constructorDependency = dependency;
    }

    @Inject
    void setDependency(Dependency dependency) {
      methodDependency = dependency;
    }

    public String message() {
      return "hello";
    }
  }

  @Singleton
  @ProvidedBy(ProvidedServiceProvider.class)
  public static class ProvidedService implements Service {
    private ProvidedService(String unused) {}
  }

  public static class ProvidedServiceProvider implements Provider<ProvidedService> {
    public ProvidedService get() {
      return new ProvidedService("provided");
    }
  }

  @Singleton
  public static class CachingProvider implements Provider<Service> {
    private final Service service = new SingletonService();

    public Service get() {
      return service;
    }
  }

  public static class FreshProvider implements Provider<Service> {
    public Service get() {
      return new SingletonService();
    }
  }
}
