/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.ws.ahc;

import akka.stream.Materializer;
import io.netty.channel.EventLoopGroup;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder;
import play.api.libs.ws.ahc.AhcConfigBuilder;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.inject.ApplicationLifecycle;
import play.libs.ws.WSAPI;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import scala.runtime.AbstractFunction1;

/**
 *
 */
@Singleton
public class AhcWSAPI implements WSAPI {

    private final AhcWSClient client;

    public AhcWSAPI(AhcWSClientConfig clientConfig, ApplicationLifecycle lifecycle, Materializer materializer) {
        this(clientConfig, lifecycle, null, materializer);
    }

    @Inject
    public AhcWSAPI(AhcWSClientConfig clientConfig, ApplicationLifecycle lifecycle, EventLoopGroup eventLoopGroup, Materializer materializer) {
        AsyncHttpClientConfig config = new AhcConfigBuilder(clientConfig).modifyUnderlying(new AbstractFunction1<Builder, Builder>() {
            @Override
            public Builder apply(Builder builder) {
                return builder.setEventLoopGroup(eventLoopGroup);
            }
        }).build();
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
