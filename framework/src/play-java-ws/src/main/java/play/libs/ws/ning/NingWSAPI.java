/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws.ning;

import java.util.concurrent.CompletableFuture;

import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder;
import play.api.libs.ws.ning.NingWSClientConfig;
import play.inject.ApplicationLifecycle;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.asynchttpclient.AsyncHttpClientConfig;

import akka.stream.Materializer;

/**
 *
 */
@Singleton
public class NingWSAPI implements WSAPI {

    private final NingWSClient client;

    @Inject
    public NingWSAPI(NingWSClientConfig clientConfig, ApplicationLifecycle lifecycle, Materializer materializer) {
        AsyncHttpClientConfig config = new NingAsyncHttpClientConfigBuilder(clientConfig).build();
        client = new NingWSClient(config, materializer);
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
