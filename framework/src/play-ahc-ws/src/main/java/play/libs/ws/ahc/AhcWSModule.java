/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import akka.stream.Materializer;
import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import play.libs.ws.WSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;
import scala.collection.Seq;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * The Play module to provide Java bindings for WS to an AsyncHTTPClient implementation.
 *
 * This binding does not bind an AsyncHttpClient instance, as it's assumed you'll use the
 * Scala and Java modules together.
 */
public class AhcWSModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
            // AsyncHttpClientProvider is added by the Scala API
            bind(WSClient.class).toProvider(AhcWSClientProvider.class)
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
