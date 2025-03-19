/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import static play.libs.F.LazySupplier.lazy;

import com.typesafe.config.Config;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.CoordinatedShutdown;
import play.api.Configuration;
import play.api.OptionalDevContext;
import play.api.OptionalSourceMapper;
import play.api.http.DefaultFileMimeTypesProvider;
import play.api.http.JavaCompatibleHttpRequestHandler;
import play.api.i18n.DefaultLangsProvider;
import play.api.inject.NewInstanceInjector$;
import play.api.libs.concurrent.ActorSystemProvider;
import play.api.libs.concurrent.CoordinatedShutdownProvider;
import play.api.mvc.request.DefaultRequestFactory;
import play.api.mvc.request.RequestFactory;
import play.core.DefaultWebCommands;
import play.core.SourceMapper;
import play.core.WebCommands;
import play.core.j.JavaHttpErrorHandlerAdapter;
import play.core.j.MappedJavaHandlerComponents;
import play.http.DefaultHttpErrorHandler;
import play.http.DefaultHttpFilters;
import play.http.HttpErrorHandler;
import play.http.HttpRequestHandler;
import play.i18n.Langs;
import play.inject.ApplicationLifecycle;
import play.libs.Files;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCSRFTokenSigner;
import play.libs.crypto.DefaultCookieSigner;
import play.mvc.BodyParser;
import play.mvc.FileMimeTypes;
import scala.jdk.javaapi.OptionConverters;

/**
 * This helper class provides all the built-in component dependencies by trading them for a single
 * dependency - the {@linkplain #context() application loader context}.
 */
public abstract class ContextBasedBuiltInComponents implements BuiltInComponents {

  // Class instances to emulate singleton behavior
  private final Supplier<Application> _application = lazy(this::createApplication);
  private final Supplier<Langs> _langs = lazy(this::createLangs);
  private final Supplier<FileMimeTypes> _fileMimeTypes = lazy(this::createFileMimeTypes);
  private final Supplier<HttpRequestHandler> _httpRequestHandler =
      lazy(this::createHttpRequestHandler);
  private final Supplier<ActorSystem> _actorSystem = lazy(this::createActorSystem);
  private final Supplier<CoordinatedShutdown> _coordinatedShutdown =
      lazy(this::createCoordinatedShutdown);
  private final Supplier<CookieSigner> _cookieSigner = lazy(this::createCookieSigner);
  private final Supplier<CSRFTokenSigner> _csrfTokenSigner = lazy(this::createCsrfTokenSigner);
  private final Supplier<Files.TemporaryFileCreator> _tempFileCreator =
      lazy(this::createTempFileCreator);

  private final Supplier<HttpErrorHandler> _httpErrorHandler = lazy(this::createHttpErrorHandler);
  private final Supplier<MappedJavaHandlerComponents> _javaHandlerComponents =
      lazy(this::createJavaHandlerComponents);
  private final Supplier<WebCommands> _webCommands = lazy(this::createWebCommands);

  /**
   * Returns the application loader context. The implementation should return a stable, effectively
   * singleton value.
   */
  public abstract ApplicationLoader.Context context();

  @Override
  public Config config() {
    return context().initialConfig();
  }

  @Override
  public Environment environment() {
    return context().environment();
  }

  @Override
  public Optional<SourceMapper> sourceMapper() {
    // Using `devContext()` method here instead of `context.sourceMapper()` because it will then
    // respect any overrides a user might define.
    return devContext().map(play.api.ApplicationLoader.DevContext::sourceMapper);
  }

  @Override
  public Optional<play.api.ApplicationLoader.DevContext> devContext() {
    return context().devContext();
  }

  @Override
  public ApplicationLifecycle applicationLifecycle() {
    return context().applicationLifecycle();
  }

  @Override
  public WebCommands webCommands() {
    // We are maintaining state for webCommands because it is a mutable object
    // where it is possible to add new handlers. Therefor the state needs to be
    // consistent everywhere it is called.
    return this._webCommands.get();
  }

  private WebCommands createWebCommands() {
    return new DefaultWebCommands();
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
            NewInstanceInjector$.MODULE$,
            configuration(),
            requestFactory,
            httpRequestHandler().asScala(),
            scalaHttpErrorHandler(),
            actorSystem(),
            materializer(),
            coordinatedShutdown())
        .asJava();
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
    return new DefaultFileMimeTypesProvider(httpConfiguration().fileMimeTypes()).get().asJava();
  }

  @Override
  public MappedJavaHandlerComponents javaHandlerComponents() {
    return this._javaHandlerComponents.get();
  }

  private MappedJavaHandlerComponents createJavaHandlerComponents() {
    MappedJavaHandlerComponents javaHandlerComponents =
        new MappedJavaHandlerComponents(
            actionCreator(), httpConfiguration(), executionContext(), javaContextComponents());

    return javaHandlerComponents
        .addBodyParser(BodyParser.Default.class, this::defaultBodyParser)
        .addBodyParser(BodyParser.AnyContent.class, this::anyContentBodyParser)
        .addBodyParser(BodyParser.Json.class, this::jsonBodyParser)
        .addBodyParser(BodyParser.TolerantJson.class, this::tolerantJsonBodyParser)
        .addBodyParser(BodyParser.Xml.class, this::xmlBodyParser)
        .addBodyParser(BodyParser.TolerantXml.class, this::tolerantXmlBodyParser)
        .addBodyParser(BodyParser.Text.class, this::textBodyParser)
        .addBodyParser(BodyParser.TolerantText.class, this::tolerantTextBodyParser)
        .addBodyParser(BodyParser.Bytes.class, this::bytesBodyParser)
        .addBodyParser(BodyParser.Raw.class, this::rawBodyParser)
        .addBodyParser(BodyParser.FormUrlEncoded.class, this::formUrlEncodedBodyParser)
        .addBodyParser(BodyParser.MultipartFormData.class, this::multipartFormDataBodyParser)
        .addBodyParser(BodyParser.Empty.class, this::emptyBodyParser);
  }

  @Override
  public HttpErrorHandler httpErrorHandler() {
    return this._httpErrorHandler.get();
  }

  private HttpErrorHandler createHttpErrorHandler() {
    return new DefaultHttpErrorHandler(
        config(),
        environment(),
        new OptionalSourceMapper(OptionConverters.toScala(sourceMapper())),
        () -> router().asScala());
  }

  @Override
  public HttpRequestHandler httpRequestHandler() {
    return this._httpRequestHandler.get();
  }

  private HttpRequestHandler createHttpRequestHandler() {
    DefaultHttpFilters filters = new DefaultHttpFilters(httpFilters());

    play.api.http.HttpErrorHandler scalaErrorHandler =
        new JavaHttpErrorHandlerAdapter(httpErrorHandler());

    return new JavaCompatibleHttpRequestHandler(
            webCommands(),
            new OptionalDevContext(OptionConverters.toScala(devContext())),
            () -> router().asScala(),
            scalaErrorHandler,
            httpConfiguration(),
            filters.asScala(),
            javaHandlerComponents())
        .asJava();
  }

  @Override
  public ActorSystem actorSystem() {
    return this._actorSystem.get();
  }

  private ActorSystem createActorSystem() {
    return new ActorSystemProvider(environment().asScala(), configuration()).get();
  }

  @Override
  public CoordinatedShutdown coordinatedShutdown() {
    return this._coordinatedShutdown.get();
  }

  private CoordinatedShutdown createCoordinatedShutdown() {
    return new CoordinatedShutdownProvider(actorSystem(), applicationLifecycle().asScala()).get();
  }

  @Override
  public CookieSigner cookieSigner() {
    return this._cookieSigner.get();
  }

  private CookieSigner createCookieSigner() {
    play.api.libs.crypto.CookieSigner scalaCookieSigner =
        new play.api.libs.crypto.DefaultCookieSigner(httpConfiguration().secret());
    return new DefaultCookieSigner(scalaCookieSigner);
  }

  @Override
  public CSRFTokenSigner csrfTokenSigner() {
    return this._csrfTokenSigner.get();
  }

  private CSRFTokenSigner createCsrfTokenSigner() {
    play.api.libs.crypto.CSRFTokenSigner scalaTokenSigner =
        new play.api.libs.crypto.DefaultCSRFTokenSigner(cookieSigner().asScala(), clock());
    return new DefaultCSRFTokenSigner(scalaTokenSigner);
  }

  @Override
  public Files.TemporaryFileCreator tempFileCreator() {
    return this._tempFileCreator.get();
  }

  private Files.TemporaryFileCreator createTempFileCreator() {
    Configuration conf = configuration();
    play.api.libs.Files.DefaultTemporaryFileReaper temporaryFileReaper =
        new play.api.libs.Files.DefaultTemporaryFileReaper(
            actorSystem(),
            play.api.libs.Files.TemporaryFileReaperConfiguration$.MODULE$.fromConfiguration(conf));

    return new play.api.libs.Files.DefaultTemporaryFileCreator(
            applicationLifecycle().asScala(), temporaryFileReaper, conf)
        .asJava();
  }
}
