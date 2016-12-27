/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import java.io.IOException;

/**
 * A WS client backed by an AsyncHttpClient.
 *
 * Normally a StandaloneAhcWSClientProvider is provided through AhcWsModule to
 * resolve dependencies here.
 */
public class AhcWSClient implements WSClient {

    private final StandaloneAhcWSClient client;

    @Inject
    public AhcWSClient(StandaloneAhcWSClient client) {
        this.client = client;
    }

    @Override
    public Object getUnderlying() {
        return client.getUnderlying();
    }

    @Override
    public WSRequest url(String url) {
        final StandaloneAhcWSRequest plainWSRequest = (StandaloneAhcWSRequest) client.url(url);
        return new AhcWSRequest(this, plainWSRequest);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
