/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import play.Environment;
import play.api.libs.ws.ahc.AsyncHttpClientProvider;
import play.components.AkkaComponents;
import play.components.ConfigurationComponents;
import play.inject.ApplicationLifecycle;
import play.libs.ws.WSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import scala.concurrent.ExecutionContext;

/**
 * AsyncHttpClient WS implementation components.
 * <p>
 * <p>Usage:</p>
 * <p>
 * <pre>
 * public class MyComponents extends BuiltInComponentsFromContext implements AhcWSComponents {
 *
 *   public MyComponents(ApplicationLoader.Context context) {
 *       super(context);
 *   }
 *
 *   // some service class that depends on WSClient
 *   public SomeService someService() {
 *       // wsClient is provided by AhcWSComponents
 *       return new SomeService(wsClient());
 *   }
 *
 *   // other methods
 * }
 * </pre>
 *
 * @see play.BuiltInComponents
 * @see WSClient
 */
public interface AhcWSComponents extends WSClientComponents, ConfigurationComponents, AkkaComponents {

    Environment environment();

    ApplicationLifecycle applicationLifecycle();

    default WSClient wsClient() {
        AsyncHttpClient asyncHttpClient = new AsyncHttpClientProvider(
                environment().asScala(),
                configuration(),
                applicationLifecycle().asScala(),
                executionContext()
        ).get();

        return new AhcWSClient(asyncHttpClient, materializer());
    }
}
