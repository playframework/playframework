/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play;

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

import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

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
          assertThat(result.status(), equalTo(Helpers.OK));
        });
  }

  @Test
  public void shouldProvideDefaultFilters() {
    assertThat(this.componentsFromContext.httpFilters().isEmpty(), is(false));
  }

  @Test
  public void shouldProvideRouter() {
    Router router = this.componentsFromContext.router();
    assertThat(router, notNullValue());

    Http.RequestHeader ok = Helpers.fakeRequest(Helpers.GET, "/").build();
    assertThat(router.route(ok).isPresent(), is(true));

    Http.RequestHeader notFound = Helpers.fakeRequest(Helpers.GET, "/404").build();
    assertThat(router.route(notFound).isPresent(), is(false));
  }

  @Test
  public void shouldProvideHttpConfiguration() {
    HttpConfiguration httpConfiguration = this.componentsFromContext.httpConfiguration();
    assertThat(httpConfiguration.context(), equalTo("/"));
    assertThat(httpConfiguration, notNullValue());
  }

  // The tests below just ensure that the we are able to instantiate the components

  @Test
  public void shouldProvideApplicationLifecycle() {
    assertThat(this.componentsFromContext.applicationLifecycle(), notNullValue());
  }

  @Test
  public void shouldProvideActionCreator() {
    assertThat(this.componentsFromContext.actionCreator(), notNullValue());
  }

  @Test
  public void shouldProvideAkkActorSystem() {
    assertThat(this.componentsFromContext.actorSystem(), notNullValue());
  }

  @Test
  public void shouldProvideAkkaMaterializer() {
    assertThat(this.componentsFromContext.materializer(), notNullValue());
  }

  @Test
  public void shouldProvideExecutionContext() {
    assertThat(this.componentsFromContext.executionContext(), notNullValue());
  }

  @Test
  public void shouldProvideCookieSigner() {
    assertThat(this.componentsFromContext.cookieSigner(), notNullValue());
  }

  @Test
  public void shouldProvideCSRFTokenSigner() {
    assertThat(this.componentsFromContext.csrfTokenSigner(), notNullValue());
  }

  @Test
  public void shouldProvideFileMimeTypes() {
    assertThat(this.componentsFromContext.fileMimeTypes(), notNullValue());
  }

  @Test
  public void shouldProvideHttpErrorHandler() {
    assertThat(this.componentsFromContext.httpErrorHandler(), notNullValue());
  }

  @Test
  public void shouldProvideHttpRequestHandler() {
    assertThat(this.componentsFromContext.httpRequestHandler(), notNullValue());
  }

  @Test
  public void shouldProvideLangs() {
    assertThat(this.componentsFromContext.langs(), notNullValue());
  }

  @Test
  public void shouldProvideMessagesApi() {
    assertThat(this.componentsFromContext.messagesApi(), notNullValue());
  }

  @Test
  public void shouldProvideTempFileCreator() {
    assertThat(this.componentsFromContext.tempFileCreator(), notNullValue());
  }

  @Test
  public void actorSystemMustBeASingleton() {
    assertThat(
        this.componentsFromContext.actorSystem(),
        sameInstance(this.componentsFromContext.actorSystem()));
  }

  @Test
  public void applicationMustBeASingleton() {
    assertThat(
        this.componentsFromContext.application(),
        sameInstance(this.componentsFromContext.application()));
  }

  @Test
  public void langsMustBeASingleton() {
    assertThat(
        this.componentsFromContext.langs(), sameInstance(this.componentsFromContext.langs()));
  }

  @Test
  public void fileMimeTypesMustBeASingleton() {
    assertThat(
        this.componentsFromContext.fileMimeTypes(),
        sameInstance(this.componentsFromContext.fileMimeTypes()));
  }

  @Test
  public void httpRequestHandlerMustBeASingleton() {
    assertThat(
        this.componentsFromContext.httpRequestHandler(),
        sameInstance(this.componentsFromContext.httpRequestHandler()));
  }

  @Test
  public void cookieSignerMustBeASingleton() {
    assertThat(
        this.componentsFromContext.cookieSigner(),
        sameInstance(this.componentsFromContext.cookieSigner()));
  }

  @Test
  public void csrfTokenSignerMustBeASingleton() {
    assertThat(
        this.componentsFromContext.csrfTokenSigner(),
        sameInstance(this.componentsFromContext.csrfTokenSigner()));
  }

  @Test
  public void temporaryFileCreatorMustBeASingleton() {
    assertThat(
        this.componentsFromContext.tempFileCreator(),
        sameInstance(this.componentsFromContext.tempFileCreator()));
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
