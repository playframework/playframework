/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import play.api.http.DefaultFileMimeTypesProvider;
import play.api.http.JavaCompatibleHttpRequestHandler;
import play.api.i18n.DefaultLangsProvider;
import play.api.inject.NewInstanceInjector$;
import play.api.inject.SimpleInjector;
import play.api.libs.concurrent.ActorSystemProvider;
import play.api.mvc.request.DefaultRequestFactory;
import play.api.mvc.request.RequestFactory;
import play.core.SourceMapper;
import play.core.j.DefaultJavaHandlerComponents;
import play.core.j.JavaHandlerComponents;
import play.core.j.JavaHttpErrorHandlerAdapter;
import play.http.DefaultHttpFilters;
import play.http.HttpRequestHandler;
import play.i18n.Langs;
import play.inject.ApplicationLifecycle;
import play.inject.DelegateInjector;
import play.inject.Injector;
import play.libs.Files;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCSRFTokenSigner;
import play.libs.crypto.DefaultCookieSigner;
import play.mvc.FileMimeTypes;
import scala.collection.immutable.Map$;

import java.util.Optional;

/**
 * Helper that provides all the built in Java components dependencies from the application loader context.
 */
public abstract class BuiltInComponentsFromContext implements BuiltInComponents {

    private final ApplicationLoader.Context context;

    // Class instances to emulate singleton behavior
    private final Application _application;
    private final Langs _langs;
    private final FileMimeTypes _fileMimeTypes;
    private final HttpRequestHandler _httpRequestHandler;
    private final ActorSystem _actorSystem;
    private final CookieSigner _cookieSigner;
    private final CSRFTokenSigner _csrfTokenSigner;
    private final Files.TemporaryFileCreator _tempFileCreator;

    private final Injector _injector;

    public BuiltInComponentsFromContext(ApplicationLoader.Context context) {
        // The order here matters
        this.context = context;

        this._injector = createInjector();
        this._actorSystem = createActorSystem();
        this._langs = createLangs();

        this._cookieSigner = createCookieSigner();
        this._csrfTokenSigner = createCsrfTokenSigner();

        this._fileMimeTypes = createFileMimeTypes();
        this._tempFileCreator = createTempFileCreator();
        this._httpRequestHandler = createHttpRequestHandler();

        this._application = createApplication();
    }

    @Override
    public Config config() {
        return context.initialConfig();
    }

    @Override
    public Environment environment() {
        return context.environment();
    }

    @Override
    public Optional<SourceMapper> sourceMapper() {
        return context.sourceMapper();
    }

    @Override
    public ApplicationLifecycle applicationLifecycle() {
        return context.applicationLifecycle();
    }

    @Override
    public Application application() {
        return this._application;
    }

    private Application createApplication() {
        RequestFactory requestFactory = new DefaultRequestFactory(httpConfiguration());
        return new play.api.DefaultApplication(
                environment().asScala(),
                applicationLifecycle().asScala(),
                injector().asScala(),
                configuration(),
                requestFactory,
                httpRequestHandler().asScala(),
                scalaHttpErrorHandler(),
                actorSystem(),
                materializer()
        ).asJava();
    }

    @Override
    public Langs langs() {
        return this._langs;
    }

    private Langs createLangs() {
        return new DefaultLangsProvider(configuration()).get().asJava();
    }

    @Override
    public FileMimeTypes fileMimeTypes() {
        return this._fileMimeTypes;
    }

    private FileMimeTypes createFileMimeTypes() {
        return new DefaultFileMimeTypesProvider(httpConfiguration().fileMimeTypes())
                .get()
                .asJava();
    }

    @Override
    public HttpRequestHandler httpRequestHandler() {
        return this._httpRequestHandler;
    }

    private HttpRequestHandler createHttpRequestHandler() {
        DefaultHttpFilters filters = new DefaultHttpFilters(httpFilters());

        JavaHandlerComponents javaHandlerComponents = new DefaultJavaHandlerComponents(
                injector().asScala(),
                actionCreator(),
                httpConfiguration(),
                executionContext(),
                javaContextComponents()
        );

        play.api.http.HttpErrorHandler scalaErrorHandler = new JavaHttpErrorHandlerAdapter(
                httpErrorHandler(),
                javaContextComponents()
        );

        return new JavaCompatibleHttpRequestHandler(
                router().asScala(),
                scalaErrorHandler,
                httpConfiguration(),
                filters.asScala(),
                javaHandlerComponents
        ).asJava();
    }

    @Override
    public ActorSystem actorSystem() {
        return this._actorSystem;
    }

    private ActorSystem createActorSystem() {
        // TODO do we need a Java version for this provider?
        return new ActorSystemProvider(
                environment().asScala(),
                configuration(),
                applicationLifecycle().asScala()
        ).get();
    }

    @Override
    public CookieSigner cookieSigner() {
        return this._cookieSigner;
    }

    private CookieSigner createCookieSigner() {
        play.api.libs.crypto.CookieSigner scalaCookieSigner = new play.api.libs.crypto.DefaultCookieSigner(httpConfiguration().secret());
        return new DefaultCookieSigner(scalaCookieSigner);
    }

    @Override
    public CSRFTokenSigner csrfTokenSigner() {
        return this._csrfTokenSigner;
    }

    private CSRFTokenSigner createCsrfTokenSigner() {
        play.api.libs.crypto.CSRFTokenSigner scalaTokenSigner = new play.api.libs.crypto.DefaultCSRFTokenSigner(
                cookieSigner().asScala(),
                clock()
        );
        return new DefaultCSRFTokenSigner(scalaTokenSigner);
    }

    @Override
    public Files.TemporaryFileCreator tempFileCreator() {
        return this._tempFileCreator;
    }

    private Files.TemporaryFileCreator createTempFileCreator() {
        play.api.libs.Files.DefaultTemporaryFileReaper temporaryFileReaper =
                new play.api.libs.Files.DefaultTemporaryFileReaper(
                        actorSystem(),
                        play.api.libs.Files.TemporaryFileReaperConfiguration$.MODULE$.fromConfiguration(configuration())
                );

        return new play.api.libs.Files.DefaultTemporaryFileCreator(
                applicationLifecycle().asScala(),
                temporaryFileReaper
        ).asJava();
    }

    @Override
    public Injector injector() {
        return this._injector;
    }

    private Injector createInjector() {
        // TODO do we need to register components like the scala version?
        SimpleInjector scalaInjector = new SimpleInjector(NewInstanceInjector$.MODULE$, Map$.MODULE$.empty());
        return new DelegateInjector(scalaInjector);
    }
}