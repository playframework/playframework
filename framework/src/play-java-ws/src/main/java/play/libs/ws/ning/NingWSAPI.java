/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws.ning;

import com.ning.http.client.AsyncHttpClientConfig;
import play.Application;
import play.api.libs.ws.DefaultWSConfigParser;
import play.api.libs.ws.WSClientConfig;
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder;
import play.libs.F;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequestHolder;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class NingWSAPI implements WSAPI {

    private final AtomicReference<F.Option<NingWSClient>> clientHolder = new AtomicReference<F.Option<NingWSClient>>(F.None());

    private Application app;

    public NingWSAPI(Application app) {
        this.app = app;
    }

    private NingWSClient newClient() {
        play.api.Configuration playConfig = app.configuration().getWrappedConfiguration();
        DefaultWSConfigParser parser = new DefaultWSConfigParser(playConfig);
        WSClientConfig clientConfig = parser.parse();
        NingAsyncHttpClientConfigBuilder builder = new NingAsyncHttpClientConfigBuilder(clientConfig, new AsyncHttpClientConfig.Builder());
        AsyncHttpClientConfig httpClientConfig = builder.build();
        return new NingWSClient(httpClientConfig);
    }

    /**
     * resets the underlying AsyncHttpClient
     */
    protected void resetClient() {
        clientHolder.getAndSet(F.None()).map(new F.Function<NingWSClient, F.Option<NingWSClient>>() {
            @Override
            public F.Option<NingWSClient> apply(NingWSClient ningWSClient) throws Throwable {
                ningWSClient.close();
                return F.Option.None();
            }
        });
    }

    @Override
    public synchronized WSClient client() {
        F.Option<NingWSClient> clientOption = clientHolder.get();
        if (clientOption.isEmpty()) {
            NingWSClient client = newClient();
            clientHolder.set(F.Some(client));
            return client;
        } else {
            return clientOption.get();
        }
    }

    @Override
    public WSRequestHolder url(String url) {
        return client().url(url);
    }
}
