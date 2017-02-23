/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc;

import akka.stream.Materializer;
import org.asynchttpclient.AsyncHttpClientConfig;
import play.api.libs.ws.ahc.AhcConfigBuilder;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.inject.ApplicationLifecycle;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 *
 */
@Singleton
public class AhcWSAPI implements WSAPI {

    private final AhcWSClient client;

    @Inject
    public AhcWSAPI(AhcWSClientConfig clientConfig, ApplicationLifecycle lifecycle, Materializer materializer) {
        AsyncHttpClientConfig config = new AhcConfigBuilder(clientConfig).build();
        client = new AhcWSClient(config, materializer);
        lifecycle.addStopHook(() -> {
            client.close();
            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public WSClient client() {
        return client;
    }

    @Override
    public WSRequest url(String url) {
        return client().url(url);
    }
}
