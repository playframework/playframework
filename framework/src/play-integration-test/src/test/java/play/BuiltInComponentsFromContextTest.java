/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import org.junit.Before;
import org.junit.Test;
import play.api.http.HttpConfiguration;
import play.components.BodyParserComponents;
import play.core.j.JavaContextComponents;
import play.filters.components.HttpFiltersComponents;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.Files;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;
import play.mvc.*;
import play.routing.Router;
import play.routing.RoutingDsl;
import play.test.Helpers;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class BuiltInComponentsFromContextTest {

    class TestBuiltInComponentsFromContext extends BuiltInComponentsFromContext implements
            HttpFiltersComponents,
            BodyParserComponents {

        TestBuiltInComponentsFromContext(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public Router router() {
            return new RoutingDsl(defaultScalaBodyParser(), javaContextComponents())
                    .GET("/").routeTo(() -> Results.ok("index"))
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
        Helpers.running(application, () -> {
            Http.RequestBuilder request = Helpers.fakeRequest(Helpers.GET, "/");
            Result result = Helpers.route(application, request);
            assertThat(result.status(), equalTo(Helpers.OK));
        });
    }

    @Test
    public void shouldProvideDefaultFilters() {
        List<EssentialFilter> filters = Arrays.asList(this.componentsFromContext.httpFilters());
        assertThat(filters.isEmpty(), is(false));
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
        assertThat(this.componentsFromContext.actorSystem(), sameInstance(this.componentsFromContext.actorSystem()));
    }

    @Test
    public void applicationMustBeASingleton() {
        assertThat(this.componentsFromContext.application(), sameInstance(this.componentsFromContext.application()));
    }

    @Test
    public void langsMustBeASingleton() {
        assertThat(this.componentsFromContext.langs(), sameInstance(this.componentsFromContext.langs()));
    }

    @Test
    public void fileMimeTypesMustBeASingleton() {
        assertThat(this.componentsFromContext.fileMimeTypes(), sameInstance(this.componentsFromContext.fileMimeTypes()));
    }

    @Test
    public void httpRequestHandlerMustBeASingleton() {
        assertThat(this.componentsFromContext.httpRequestHandler(), sameInstance(this.componentsFromContext.httpRequestHandler()));
    }

    @Test
    public void cookieSignerMustBeASingleton() {
        assertThat(this.componentsFromContext.cookieSigner(), sameInstance(this.componentsFromContext.cookieSigner()));
    }

    @Test
    public void csrfTokenSignerMustBeASingleton() {
        assertThat(this.componentsFromContext.csrfTokenSigner(), sameInstance(this.componentsFromContext.csrfTokenSigner()));
    }

    @Test
    public void temporaryFileCreatorMustBeASingleton() {
        assertThat(this.componentsFromContext.tempFileCreator(), sameInstance(this.componentsFromContext.tempFileCreator()));
    }

    // Injector tests

    @Test
    public void shouldBeAbleToInjectHttpConfiguration() {
        assertThat(this.componentsFromContext.injector().instanceOf(HttpConfiguration.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectJavaContextComponents() {
        assertThat(this.componentsFromContext.injector().instanceOf(JavaContextComponents.class), notNullValue());
    }


    @Test
    public void shouldBeAbleToInjectCookieSigner() {
        assertThat(this.componentsFromContext.injector().instanceOf(CookieSigner.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectCSRFTokenSigner() {
        assertThat(this.componentsFromContext.injector().instanceOf(CSRFTokenSigner.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectTemporaryFileCreator() {
        assertThat(this.componentsFromContext.injector().instanceOf(Files.TemporaryFileCreator.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectFileMimeTypes() {
        assertThat(this.componentsFromContext.injector().instanceOf(FileMimeTypes.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectMessagesApi() {
        assertThat(this.componentsFromContext.injector().instanceOf(MessagesApi.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectLangs() {
        assertThat(this.componentsFromContext.injector().instanceOf(Langs.class), notNullValue());
    }


    @Test
    public void shouldBeAbleToInjectScalaVersionOfCookieSigner() {
        assertThat(this.componentsFromContext.injector().instanceOf(play.api.libs.crypto.CookieSigner.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectScalaVersionOfCSRFTokenSigner() {
        assertThat(this.componentsFromContext.injector().instanceOf(play.api.libs.crypto.CSRFTokenSigner.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectScalaVersionOfTemporaryFileCreator() {
        assertThat(this.componentsFromContext.injector().instanceOf(play.api.libs.Files.TemporaryFileCreator.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectScalaVersionOfFileMimeTypes() {
        assertThat(this.componentsFromContext.injector().instanceOf(play.api.http.FileMimeTypes.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectScalaVersionOfMessagesApi() {
        assertThat(this.componentsFromContext.injector().instanceOf(play.api.i18n.MessagesApi.class), notNullValue());
    }

    @Test
    public void shouldBeAbleToInjectScalaVersionOfLangs() {
        assertThat(this.componentsFromContext.injector().instanceOf(play.api.i18n.Langs.class), notNullValue());
    }
}
