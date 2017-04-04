/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import akka.actor.ActorSystem;
import play.api.OptionalSourceMapper;
import play.api.http.DefaultFileMimeTypesProvider;
import play.api.http.HttpConfiguration;
import play.api.http.JavaCompatibleHttpRequestHandler;
import play.api.i18n.DefaultLangsProvider;
import play.api.i18n.DefaultMessagesApiProvider;
import play.api.inject.RoutesProvider;
import play.api.libs.concurrent.ActorSystemProvider;
import play.api.libs.crypto.DefaultCookieSigner;
import play.api.mvc.request.DefaultRequestFactory;
import play.api.mvc.request.RequestFactory;
import play.components.*;
import play.core.j.*;
import play.http.*;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.Files;
import play.libs.crypto.CSRFTokenSigner;
import play.libs.crypto.CookieSigner;
import play.libs.crypto.DefaultCSRFTokenSigner;
import play.libs.crypto.HMACSHA1CookieSigner;
import play.mvc.FileMimeTypes;
import scala.compat.java8.OptionConverters;

import javax.inject.Provider;

/**
 * Helper to provide the Play built in components.
 */
public interface BuiltInComponents extends BaseComponents,
        ApplicationComponents,
        JavaContextComponentsComponents,
        AkkaComponents,
        ConfigurationComponents,
        HttpComponents,
        HttpErrorHandlerComponents,
        I18nComponents,
        CryptoComponents,
        TemporaryFileComponents {

    @Override
    default Application application() {
        play.api.Environment env = environment().asScala();

        play.api.http.HttpErrorHandler scalaErrorHandler = new JavaHttpErrorHandlerAdapter(
            httpErrorHandler(),
            javaContextComponents()
        );

        RequestFactory requestFactory = new DefaultRequestFactory(httpConfiguration());
        return new play.api.DefaultApplication(
            env,
            applicationLifecycle().asScala(),
            injector().asScala(),
            configuration(),
            requestFactory,
            httpRequestHandler().asScala(),
            scalaErrorHandler,
            actorSystem(),
            materializer()
        ).asJava();
    }

    @Override
    default JavaContextComponents javaContextComponents() {
        return JavaHelpers$.MODULE$.createContextComponents(
                messagesApi().asScala(),
                langs().asScala(),
                fileMimeTypes().asScala(),
                httpConfiguration()
        );
    }

    @Override
    default Langs langs() {
        return new DefaultLangsProvider(configuration()).get().asJava();
    }

    @Override
    default MessagesApi messagesApi() {
        return new DefaultMessagesApiProvider(
                environment().asScala(),
                configuration(),
                langs().asScala(),
                httpConfiguration()
        ).get().asJava();
    }

    @Override
    default FileMimeTypes fileMimeTypes() {
        return new DefaultFileMimeTypesProvider(httpConfiguration().fileMimeTypes())
                .get()
                .asJava();
    }

    @Override
    default ActionCreator actionCreator() {
        return new DefaultActionCreator();
    }

    @Override
    default HttpConfiguration httpConfiguration() {
        return HttpConfiguration.fromConfiguration(configuration(), environment().asScala());
    }

    @Override
    default HttpErrorHandler httpErrorHandler() {
        Provider<play.api.routing.Router> provider = new RoutesProvider(
                injector().asScala(),
                environment().asScala(),
                configuration(),
                httpConfiguration()
        );
        return new DefaultHttpErrorHandler(
                config(),
                environment(),
                new OptionalSourceMapper(OptionConverters.toScala(sourceMapper())),
                provider
        );
    }

    @Override
    default HttpRequestHandler httpRequestHandler() {
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
    default ActorSystem actorSystem() {
        // TODO do we need a Java version for this provider?
        return new ActorSystemProvider(
                environment().asScala(),
                configuration(),
                applicationLifecycle().asScala()
        ).get();
    }

    @Override
    default CookieSigner cookieSigner() {
        play.api.libs.crypto.CookieSigner scalaCookieSigner = new DefaultCookieSigner(httpConfiguration().secret());
        return new HMACSHA1CookieSigner(scalaCookieSigner);
    }

    @Override
    default CSRFTokenSigner csrfTokenSigner() {
        play.api.libs.crypto.CSRFTokenSigner scalaTokenSigner = new play.api.libs.crypto.DefaultCSRFTokenSigner(
                cookieSigner().asScala(),
                clock()
        );
        return new DefaultCSRFTokenSigner(scalaTokenSigner);
    }

    @Override
    default Files.TemporaryFileCreator tempFileCreator() {
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
}