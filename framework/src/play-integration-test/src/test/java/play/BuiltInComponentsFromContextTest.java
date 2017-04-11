/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import org.junit.Before;
import org.junit.Test;
import play.api.http.HttpConfiguration;
import play.components.BodyParserComponents;
import play.filters.components.HttpFiltersComponents;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
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

        public TestBuiltInComponentsFromContext(ApplicationLoader.Context context) {
            super(context);
        }

        @Override
        public Router router() {
            return new RoutingDsl(defaultScalaParser(), javaContextComponents())
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
}
