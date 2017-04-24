/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import play.api.inject.NewInstanceInjector$;
import play.api.inject.SimpleInjector;
import play.core.SourceMapper;
import play.http.HttpRequestHandler;
import play.i18n.Langs;
import play.inject.ApplicationLifecycle;
import play.inject.DelegateInjector;
import play.inject.Injector;
import play.libs.Files;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;
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
        this.context = context;

        this._application = application();
        this._langs = langs();
        this._fileMimeTypes = fileMimeTypes();
        this._httpRequestHandler = httpRequestHandler();
        this._actorSystem = actorSystem();
        this._cookieSigner = cookieSigner();
        this._csrfTokenSigner = csrfTokenSigner();
        this._tempFileCreator = tempFileCreator();

        this._injector = createInjector();
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
    @Override
    public Langs langs() {
        return this._langs;
    }

    @Override
    public FileMimeTypes fileMimeTypes() {
        return this._fileMimeTypes;
    }

    @Override
    public HttpRequestHandler httpRequestHandler() {
        return this._httpRequestHandler;
    }

    @Override
    public ActorSystem actorSystem() {
        return this._actorSystem;
    }

    @Override
    public CookieSigner cookieSigner() {
        return this._cookieSigner;
    }

    @Override
    public CSRFTokenSigner csrfTokenSigner() {
        return this._csrfTokenSigner;
    }

    @Override
    public Files.TemporaryFileCreator tempFileCreator() {
        return this._tempFileCreator;
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