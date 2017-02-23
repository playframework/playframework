/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import scala.collection.Seq;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

public class AhcWSModule extends Module {

    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
                bind(WSAPI.class).to(AhcWSAPI.class),
                bind(WSClient.class).toProvider(WSClientProvider.class)
        );
    }

    @Singleton
    public static class WSClientProvider implements Provider<WSClient> {
        private final WSAPI wsApi;

        @Inject
        public WSClientProvider(WSAPI wsApi) {
            this.wsApi = wsApi;
        }

        @Override
        public WSClient get() {
            return wsApi.client();
        }
    }

}
