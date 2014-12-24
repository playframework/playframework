/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws.ning;

import com.ning.http.client.AsyncHttpClientConfig;
import play.Application;
import play.Environment;
import play.api.libs.ws.DefaultWSConfigParser;
import play.api.libs.ws.WSClientConfig;
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder;
import play.api.libs.ws.ning.NingWSClientConfig;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequestHolder;
import scala.Unit;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

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
    public WSRequestHolder url(String url) {
        return client().url(url);
    }
}
