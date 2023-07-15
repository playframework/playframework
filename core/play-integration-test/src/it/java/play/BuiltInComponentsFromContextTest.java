/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class BuiltInComponentsFromContextTest {

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

  @BeforeEach
  public void initialize() {
    ApplicationLoader.Context context = ApplicationLoader.create(Environment.simple());
    this.componentsFromContext = new TestBuiltInComponentsFromContext(context);
  }

  @Test
  void shouldProvideAApplication() {
    Application application = componentsFromContext.application();
    Helpers.running(
        application,
        () -> {
          Http.RequestBuilder request = Helpers.fakeRequest(Helpers.GET, "/");
          Result result = Helpers.route(application, request);
          assertEquals(Helpers.OK, result.status());
        });
  }

  @Test
  void shouldProvideDefaultFilters() {
    assertFalse(this.componentsFromContext.httpFilters().isEmpty());
  }

  @Test
  void shouldProvideRouter() {
    Router router = this.componentsFromContext.router();
    assertNotNull(router);

    Http.RequestHeader ok = Helpers.fakeRequest(Helpers.GET, "/").build();
    assertTrue(router.route(ok).isPresent());

    Http.RequestHeader notFound = Helpers.fakeRequest(Helpers.GET, "/404").build();
    assertFalse(router.route(notFound).isPresent());
  }

  @Test
  void shouldProvideHttpConfiguration() {
    HttpConfiguration httpConfiguration = this.componentsFromContext.httpConfiguration();
    assertNotNull(httpConfiguration);
    assertEquals("/", httpConfiguration.context());
  }

  // The tests below just ensure that the we are able to instantiate the components

  @Test
  void shouldProvideApplicationLifecycle() {
    assertNotNull(this.componentsFromContext.applicationLifecycle());
  }

  @Test
  void shouldProvideActionCreator() {
    assertNotNull(this.componentsFromContext.actionCreator());
  }

  @Test
  void shouldProvideAkkActorSystem() {
    assertNotNull(this.componentsFromContext.actorSystem());
  }

  @Test
  void shouldProvideAkkaMaterializer() {
    assertNotNull(this.componentsFromContext.materializer());
  }

  @Test
  void shouldProvideExecutionContext() {
    assertNotNull(this.componentsFromContext.executionContext());
  }

  @Test
  void shouldProvideCookieSigner() {
    assertNotNull(this.componentsFromContext.cookieSigner());
  }

  @Test
  void shouldProvideCSRFTokenSigner() {
    assertNotNull(this.componentsFromContext.csrfTokenSigner());
  }

  @Test
  void shouldProvideFileMimeTypes() {
    assertNotNull(this.componentsFromContext.fileMimeTypes());
  }

  @Test
  void shouldProvideHttpErrorHandler() {
    assertNotNull(this.componentsFromContext.httpErrorHandler());
  }

  @Test
  void shouldProvideHttpRequestHandler() {
    assertNotNull(this.componentsFromContext.httpRequestHandler());
  }

  @Test
  void shouldProvideLangs() {
    assertNotNull(this.componentsFromContext.langs());
  }

  @Test
  void shouldProvideMessagesApi() {
    assertNotNull(this.componentsFromContext.messagesApi());
  }

  @Test
  void shouldProvideTempFileCreator() {
    assertNotNull(this.componentsFromContext.tempFileCreator());
  }

  @Test
  void actorSystemMustBeASingleton() {
    assertSame(this.componentsFromContext.actorSystem(), this.componentsFromContext.actorSystem());
  }

  @Test
  void applicationMustBeASingleton() {
    assertSame(this.componentsFromContext.application(), this.componentsFromContext.application());
  }

  @Test
  void langsMustBeASingleton() {
    assertSame(this.componentsFromContext.langs(), this.componentsFromContext.langs());
  }

  @Test
  void fileMimeTypesMustBeASingleton() {
    assertSame(
        this.componentsFromContext.fileMimeTypes(), this.componentsFromContext.fileMimeTypes());
  }

  @Test
  void httpRequestHandlerMustBeASingleton() {
    assertSame(
        this.componentsFromContext.httpRequestHandler(),
        this.componentsFromContext.httpRequestHandler());
  }

  @Test
  void cookieSignerMustBeASingleton() {
    assertSame(
        this.componentsFromContext.cookieSigner(), this.componentsFromContext.cookieSigner());
  }

  @Test
  void csrfTokenSignerMustBeASingleton() {
    assertSame(
        this.componentsFromContext.csrfTokenSigner(), this.componentsFromContext.csrfTokenSigner());
  }

  @Test
  void temporaryFileCreatorMustBeASingleton() {
    assertSame(
        this.componentsFromContext.tempFileCreator(), this.componentsFromContext.tempFileCreator());
  }

  @Test
  void shouldKeepStateForWebCommands() {
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
