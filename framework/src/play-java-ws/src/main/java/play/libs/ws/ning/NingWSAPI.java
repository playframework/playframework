/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs.ws.ning;

import com.ning.http.client.AsyncHttpClientConfig;
import play.Application;
import play.Configuration;
import play.libs.F;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequestHolder;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
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
        Configuration playConfig = app.configuration();
        AsyncHttpClientConfig.Builder asyncHttpConfig = new AsyncHttpClientConfig.Builder()
                .setConnectionTimeoutInMs(playConfig.getMilliseconds("ws.timeout.connection", 120000L).intValue())
                .setIdleConnectionTimeoutInMs(playConfig.getMilliseconds("ws.timeout.idle", 120000L).intValue())
                .setRequestTimeoutInMs(playConfig.getMilliseconds("ws.timeout.request", 120000L).intValue())
                .setFollowRedirects(playConfig.getBoolean("ws.followRedirects", true).booleanValue())
                .setUseProxyProperties(playConfig.getBoolean("ws.useProxyProperties", true))
                .setCompressionEnabled(playConfig.getBoolean("ws.compressionEnabled", false));

        String userAgent = playConfig.getString("ws.useragent");
        if (userAgent != null) {
            asyncHttpConfig.setUserAgent(userAgent);
        }

        if (!playConfig.getBoolean("ws.acceptAnyCertificate", false).booleanValue()) {
            try {
                asyncHttpConfig.setSSLContext(SSLContext.getDefault());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
        return new NingWSClient(asyncHttpConfig.build());
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
