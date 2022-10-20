/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import org.junit.Test;
import play.api.libs.ws.WSConfigParser;
import play.api.libs.ws.ahc.AhcConfigBuilder;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.api.libs.ws.ahc.AhcWSClientConfigFactory;
import play.libs.ws.WSClient;
import play.libs.ws.ahc.AhcWSClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;

public class StandaloneWithConfig {

  @Test
  public void testMe() throws IOException {
    // #ws-standalone-with-config
    // Set up Akka
    String name = "wsclient";
    ActorSystem system = ActorSystem.create(name);
    Materializer materializer = Materializer.matFromSystem(system);

    // Read in config file from application.conf
    Config conf = ConfigFactory.load();
    WSConfigParser parser = new WSConfigParser(conf, ClassLoader.getSystemClassLoader());
    AhcWSClientConfig clientConf = AhcWSClientConfigFactory.forClientConfig(parser.parse());

    // Start up asynchttpclient
    final DefaultAsyncHttpClientConfig asyncHttpClientConfig =
        new AhcConfigBuilder(clientConf).configure().build();
    final DefaultAsyncHttpClient asyncHttpClient =
        new DefaultAsyncHttpClient(asyncHttpClientConfig);

    // Create a new WSClient, and then close the client.
    WSClient client = new AhcWSClient(asyncHttpClient, materializer);
    client.close();
    system.terminate();
    // #ws-standalone-with-config
  }
}
