/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import play.api.http.HttpConfiguration;
import play.api.i18n.DefaultMessagesApiProvider;
import play.components.*;
import play.core.DefaultWebCommands;
import play.core.WebCommands;
import play.core.j.JavaContextComponents;
import play.core.j.JavaHelpers$;
import play.http.ActionCreator;
import play.http.DefaultActionCreator;
import play.i18n.I18nComponents;
import play.i18n.MessagesApi;

import java.util.Optional;

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

    /**
     * Commands that intercept requests before the rest of the application handles them. Used by Evolutions.
     *
     * @return the application web commands.
     */
    default WebCommands webCommands() {
        return new DefaultWebCommands();
    }

    /**
     * Helper to interact with the Play build environment. Only available in dev mode.
     */
    default Optional<play.api.ApplicationLoader.DevContext> devContext() {
        return Optional.empty();
    }

}