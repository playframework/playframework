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
import java.util.function.Supplier;

import static play.libs.F.LazySupplier.lazy;

/**
 * Helper that provides all the built in Java components dependencies from the application loader context.
 */
public abstract class BuiltInComponentsFromContext implements BuiltInComponents {

    private final ApplicationLoader.Context context;

    // Class instances to emulate singleton behavior
    private final Supplier<Application> _application = lazy(this::createApplication);
    private final Supplier<Langs> _langs = lazy(this::createLangs);
    private final Supplier<FileMimeTypes> _fileMimeTypes = lazy(this::createFileMimeTypes);
    private final Supplier<HttpRequestHandler> _httpRequestHandler = lazy(this::createHttpRequestHandler);
    private final Supplier<ActorSystem> _actorSystem = lazy(this::createActorSystem);
    private final Supplier<CookieSigner> _cookieSigner = lazy(this::createCookieSigner);
    private final Supplier<CSRFTokenSigner> _csrfTokenSigner = lazy(this::createCsrfTokenSigner);
    private final Supplier<Files.TemporaryFileCreator> _tempFileCreator = lazy(this::createTempFileCreator);

    private final Supplier<Injector> _injector = lazy(this::createInjector);

    public BuiltInComponentsFromContext(ApplicationLoader.Context context) {
        this.context = context;
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
        return this._application.get();
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
        return this._langs.get();
    }

    private Langs createLangs() {
        return new DefaultLangsProvider(configuration()).get().asJava();
    }

    @Override
    public FileMimeTypes fileMimeTypes() {
        return this._fileMimeTypes.get();
    }

    private FileMimeTypes createFileMimeTypes() {
        return new DefaultFileMimeTypesProvider(httpConfiguration().fileMimeTypes())
                .get()
                .asJava();
    }

    @Override
    public HttpRequestHandler httpRequestHandler() {
        return this._httpRequestHandler.get();
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
        return this._actorSystem.get();
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
        return this._cookieSigner.get();
    }

    private CookieSigner createCookieSigner() {
        play.api.libs.crypto.CookieSigner scalaCookieSigner = new play.api.libs.crypto.DefaultCookieSigner(httpConfiguration().secret());
        return new DefaultCookieSigner(scalaCookieSigner);
    }

    @Override
    public CSRFTokenSigner csrfTokenSigner() {
        return this._csrfTokenSigner.get();
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
        return this._tempFileCreator.get();
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
        return this._injector.get();
    }

    private Injector createInjector() {
        // TODO do we need to register components like the scala version?
        SimpleInjector scalaInjector = new SimpleInjector(NewInstanceInjector$.MODULE$, Map$.MODULE$.empty());
        return new DelegateInjector(scalaInjector);
    }
}