/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import akka.stream.Materializer;
import com.typesafe.config.Config;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;
import play.libs.ws.WSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

/**
 * The Play module to provide Java bindings for WS to an AsyncHTTPClient implementation.
 *
 * This binding does not bind an AsyncHttpClient instance, as it's assumed you'll use the
 * Scala and Java modules together.
 */
public class AhcWSModule extends Module {

    @Override
    public List<Binding<?>> bindings(final Environment environment, final Config config) {
        return Collections.singletonList(
            // AsyncHttpClientProvider is added by the Scala API
            bindClass(WSClient.class).toProvider(AhcWSClientProvider.class)
        );
    }

    @Singleton
    public static class AhcWSClientProvider implements Provider<WSClient> {
        private final AhcWSClient client;

        @Inject
        public AhcWSClientProvider(AsyncHttpClient asyncHttpClient, Materializer materializer) {
            client = new AhcWSClient(new StandaloneAhcWSClient(asyncHttpClient, materializer), materializer);
        }

        @Override
        public WSClient get() {
            return client;
        }

    }

}
