/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc;

import akka.stream.Materializer;
import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import play.libs.ws.WSClient;
import scala.collection.Seq;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

public class AhcWSModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
                // AsyncHttpClientProvider is added by the Scala API
                bind(StandaloneAhcWSClient.class).toProvider(StandaloneAhcWSClientProvider.class),
                bind(WSClient.class).toProvider(WSClientProvider.class)
        );
    }

    @Singleton
    public static class StandaloneAhcWSClientProvider implements Provider<StandaloneAhcWSClient> {
        private final StandaloneAhcWSClient standaloneAhcWSClient;

        @Inject
        public StandaloneAhcWSClientProvider(play.shaded.ahc.org.asynchttpclient.AsyncHttpClient asyncHttpClient, Materializer materializer) {
            standaloneAhcWSClient = new StandaloneAhcWSClient(asyncHttpClient, materializer);
        }

        @Override
        public StandaloneAhcWSClient get() {
            return standaloneAhcWSClient;
        }
    }

    @Singleton
    public static class WSClientProvider implements Provider<WSClient> {
        private final AhcWSClient client;

        @Inject
        public WSClientProvider(StandaloneAhcWSClient plainAhcWSClient) {
            client = new AhcWSClient(plainAhcWSClient);
        }

        @Override
        public WSClient get() {
            return client;
        }

    }

}
