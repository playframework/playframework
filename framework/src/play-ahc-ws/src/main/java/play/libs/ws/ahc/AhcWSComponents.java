/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc;

import play.Environment;
import play.api.libs.ws.ahc.AsyncHttpClientProvider;
import play.components.AkkaComponents;
import play.components.ConfigurationComponents;
import play.inject.ApplicationLifecycle;
import play.libs.ws.WSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

/**
 * AsyncHttpClient WS implementation components.
 *
 * @see play.BuiltInComponents
 * @see WSClient
 */
public interface AhcWSComponents extends ConfigurationComponents, AkkaComponents {

    Environment environment();

    ApplicationLifecycle applicationLifecycle();

    default WSClient wsClient() {
        AsyncHttpClient asyncHttpClient = new AsyncHttpClientProvider(
            configuration(),
            environment().asScala(),
            applicationLifecycle().asScala()
        ).get();

        return new AhcWSClient(new StandaloneAhcWSClient(asyncHttpClient, materializer()));
    }
}
