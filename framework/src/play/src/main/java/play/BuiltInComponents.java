/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play;

import play.api.OptionalSourceMapper;
import play.api.http.HttpConfiguration;
import play.api.i18n.DefaultMessagesApiProvider;
import play.api.inject.RoutesProvider;
import play.components.*;
import play.core.j.JavaContextComponents;
import play.core.j.JavaHelpers$;
import play.http.ActionCreator;
import play.http.DefaultActionCreator;
import play.http.DefaultHttpErrorHandler;
import play.http.HttpErrorHandler;
import play.i18n.I18nComponents;
import play.i18n.MessagesApi;
import scala.compat.java8.OptionConverters;

import javax.inject.Provider;

/**
 * Helper to provide the Play built in components.
 */
public interface BuiltInComponents extends
        AkkaComponents,
        ApplicationComponents,
        BaseComponents,
        BodyParserComponents,
        ConfigurationComponents,
        CryptoComponents,
        FileMimeTypesComponents,
        HttpComponents,
        HttpErrorHandlerComponents,
        I18nComponents,
        TemporaryFileComponents {

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
    default MessagesApi messagesApi() {
        return new DefaultMessagesApiProvider(
                environment().asScala(),
                configuration(),
                langs().asScala(),
                httpConfiguration()
        ).get().asJava();
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
}