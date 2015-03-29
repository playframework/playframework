/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws.ning;

import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder;
import play.api.libs.ws.ning.NingWSClientConfig;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;

/**
 *
 */
@Singleton
public class NingWSAPI implements WSAPI {

    private final NingWSClient client;

    @Inject
    public NingWSAPI(NingWSClientConfig clientConfig, ApplicationLifecycle lifecycle) {
        client = new NingWSClient(
                new NingAsyncHttpClientConfigBuilder(clientConfig).build()
        );
        lifecycle.addStopHook(new Callable<F.Promise<Void>>() {
            @Override
            public F.Promise<Void> call() throws Exception {
                client.close();
                return F.Promise.pure(null);
            }
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
