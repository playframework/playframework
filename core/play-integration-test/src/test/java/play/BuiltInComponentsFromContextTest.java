/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import org.junit.Before;
import org.junit.Test;
import play.api.http.HttpConfiguration;
import play.api.mvc.RequestHeader;
import play.components.BodyParserComponents;
import play.core.BuildLink;
import play.core.HandleWebCommandSupport;
import play.filters.components.HttpFiltersComponents;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.routing.Router;
import play.routing.RoutingDsl;
import play.test.Helpers;
import scala.Option;

public class BuiltInComponentsFromContextTest {

  class TestBuiltInComponentsFromContext extends BuiltInComponentsFromContext
      implements HttpFiltersComponents, BodyParserComponents {

    TestBuiltInComponentsFromContext(ApplicationLoader.Context context) {
      super(context);
    }

    @Override
    public Router router() {
      return new RoutingDsl(defaultBodyParser())
          .GET("/")
          .routingTo(req -> Results.ok("index"))
          .build();
    }
  }

  private BuiltInComponentsFromContext componentsFromContext;

  @Before
  public void initialize() {
    ApplicationLoader.Context context = ApplicationLoader.create(Environment.simple());
    this.componentsFromContext = new TestBuiltInComponentsFromContext(context);
  }

  @Test
  public void shouldProvideAApplication() {
    Application application = componentsFromContext.application();
    Helpers.running(
        application,
        () -> {
          Http.RequestBuilder request = Helpers.fakeRequest(Helpers.GET, "/");
          Result result = Helpers.route(application, request);
          assertThat(result.status()).isEqualTo(Helpers.OK);
        });
  }

  @Test
  public void shouldProvideDefaultFilters() {
    assertThat(this.componentsFromContext.httpFilters()).isNotEmpty();
  }

  @Test
  public void shouldProvideRouter() {
    Router router = this.componentsFromContext.router();
    assertThat(router).isNotNull();

    Http.RequestHeader ok = Helpers.fakeRequest(Helpers.GET, "/").build();
    assertThat(router.route(ok)).isPresent();

    Http.RequestHeader notFound = Helpers.fakeRequest(Helpers.GET, "/404").build();
    assertThat(router.route(notFound)).isNotPresent();
  }

  @Test
  public void shouldProvideHttpConfiguration() {
    HttpConfiguration httpConfiguration = this.componentsFromContext.httpConfiguration();
    assertThat(httpConfiguration.context()).isEqualTo("/");
    assertThat(httpConfiguration).isNotNull();
  }

  // The tests below just ensure that the we are able to instantiate the components

  @Test
  public void shouldProvideApplicationLifecycle() {
    assertThat(this.componentsFromContext.applicationLifecycle()).isNotNull();
  }

  @Test
  public void shouldProvideActionCreator() {
    assertThat(this.componentsFromContext.actionCreator()).isNotNull();
  }

  @Test
  public void shouldProvidePekkoActorSystem() {
    assertThat(this.componentsFromContext.actorSystem()).isNotNull();
  }

  @Test
  public void shouldProvidePekkoMaterializer() {
    assertThat(this.componentsFromContext.materializer()).isNotNull();
  }

  @Test
  public void shouldProvideExecutionContext() {
    assertThat(this.componentsFromContext.executionContext()).isNotNull();
  }

  @Test
  public void shouldProvideCookieSigner() {
    assertThat(this.componentsFromContext.cookieSigner()).isNotNull();
  }

  @Test
  public void shouldProvideCSRFTokenSigner() {
    assertThat(this.componentsFromContext.csrfTokenSigner()).isNotNull();
  }

  @Test
  public void shouldProvideFileMimeTypes() {
    assertThat(this.componentsFromContext.fileMimeTypes()).isNotNull();
  }

  @Test
  public void shouldProvideHttpErrorHandler() {
    assertThat(this.componentsFromContext.httpErrorHandler()).isNotNull();
  }

  @Test
  public void shouldProvideHttpRequestHandler() {
    assertThat(this.componentsFromContext.httpRequestHandler()).isNotNull();
  }

  @Test
  public void shouldProvideLangs() {
    assertThat(this.componentsFromContext.langs()).isNotNull();
  }

  @Test
  public void shouldProvideMessagesApi() {
    assertThat(this.componentsFromContext.messagesApi()).isNotNull();
  }

  @Test
  public void shouldProvideTempFileCreator() {
    assertThat(this.componentsFromContext.tempFileCreator()).isNotNull();
  }

  @Test
  public void actorSystemMustBeASingleton() {
    assertThat(this.componentsFromContext.actorSystem())
        .isSameAs(this.componentsFromContext.actorSystem());
  }

  @Test
  public void applicationMustBeASingleton() {
    assertThat(this.componentsFromContext.application())
        .isSameAs(this.componentsFromContext.application());
  }

  @Test
  public void langsMustBeASingleton() {
    assertThat(this.componentsFromContext.langs()).isSameAs(this.componentsFromContext.langs());
  }

  @Test
  public void fileMimeTypesMustBeASingleton() {
    assertThat(this.componentsFromContext.fileMimeTypes())
        .isSameAs(this.componentsFromContext.fileMimeTypes());
  }

  @Test
  public void httpRequestHandlerMustBeASingleton() {
    assertThat(this.componentsFromContext.httpRequestHandler())
        .isSameAs(this.componentsFromContext.httpRequestHandler());
  }

  @Test
  public void cookieSignerMustBeASingleton() {
    assertThat(this.componentsFromContext.cookieSigner())
        .isSameAs(this.componentsFromContext.cookieSigner());
  }

  @Test
  public void csrfTokenSignerMustBeASingleton() {
    assertThat(this.componentsFromContext.csrfTokenSigner())
        .isSameAs(this.componentsFromContext.csrfTokenSigner());
  }

  @Test
  public void temporaryFileCreatorMustBeASingleton() {
    assertThat(this.componentsFromContext.tempFileCreator())
        .isSameAs(this.componentsFromContext.tempFileCreator());
  }

  @Test
  public void shouldKeepStateForWebCommands() {
    componentsFromContext
        .webCommands()
        .addHandler(
            new HandleWebCommandSupport() {
              @Override
              public Option<play.api.mvc.Result> handleWebCommand(
                  RequestHeader request, BuildLink buildLink, File path) {
                // We don't care at this test what the handler is doing.
                // So we can throw an exception and check against it to
                // verify that the components are maintaining its state.
                throw new RuntimeException("Expected");
              }
            });

    try {
      // We also don't care about the parameters
      componentsFromContext.webCommands().handleWebCommand(null, null, null);
      fail("Should throw an exception");
    } catch (RuntimeException ex) {
      assertEquals("Expected", ex.getMessage());
    }
  }
}
