/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play;

import play.api.http.HttpConfiguration;
import play.api.i18n.DefaultMessagesApiProvider;
import play.components.*;
import play.core.j.JavaContextComponents;
import play.core.j.JavaHelpers$;
import play.http.ActionCreator;
import play.http.DefaultActionCreator;
import play.i18n.I18nComponents;
import play.i18n.MessagesApi;

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
}