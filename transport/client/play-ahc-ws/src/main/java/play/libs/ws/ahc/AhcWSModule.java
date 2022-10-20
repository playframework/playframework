/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.ws.ahc;

import akka.stream.Materializer;
import com.typesafe.config.Config;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;
import play.libs.ws.WSClient;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClient;

/**
 * The Play module to provide Java bindings for WS to an AsyncHTTPClient implementation.
 *
 * <p>This binding does not bind an AsyncHttpClient instance, as it's assumed you'll use the Scala
 * and Java modules together.
 */
public class AhcWSModule extends Module {

  @Override
  public List<Binding<?>> bindings(final Environment environment, final Config config) {
    return Collections.singletonList(
        // AsyncHttpClientProvider is added by the Scala API
        bindClass(WSClient.class).toProvider(AhcWSClientProvider.class));
  }

  @Singleton
  public static class AhcWSClientProvider implements Provider<WSClient> {
    private final AhcWSClient client;

    @Inject
    public AhcWSClientProvider(AsyncHttpClient asyncHttpClient, Materializer materializer) {
      client =
          new AhcWSClient(new StandaloneAhcWSClient(asyncHttpClient, materializer), materializer);
    }

    @Override
    public WSClient get() {
      return client;
    }
  }
}
