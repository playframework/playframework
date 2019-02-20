/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.ws;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import play.api.libs.ws.WSConfigParser;
import play.api.libs.ws.ahc.AhcConfigBuilder;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.api.libs.ws.ahc.AhcWSClientConfigFactory;
import play.libs.ws.WSClient;
import play.libs.ws.ahc.AhcWSClient;
import play.libs.ws.ahc.StandaloneAhcWSClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;

public class StandaloneWithConfig {

  @Test
  public void testMe() throws IOException {
    // #ws-standalone-with-config
    // Set up Akka
    String name = "wsclient";
    ActorSystem system = ActorSystem.create(name);
    ActorMaterializerSettings settings = ActorMaterializerSettings.create(system);
    ActorMaterializer materializer = ActorMaterializer.create(settings, system, name);

    // Read in config file from application.conf
    Config conf = ConfigFactory.load();
    WSConfigParser parser = new WSConfigParser(conf, ClassLoader.getSystemClassLoader());
    AhcWSClientConfig clientConf = AhcWSClientConfigFactory.forClientConfig(parser.parse());

    // Start up asynchttpclient
    final DefaultAsyncHttpClientConfig asyncHttpClientConfig =
        new AhcConfigBuilder(clientConf).configure().build();
    final DefaultAsyncHttpClient asyncHttpClient =
        new DefaultAsyncHttpClient(asyncHttpClientConfig);

    // Create a new WS client, and then close the client.
    WSClient client = new AhcWSClient(asyncHttpClient, materializer);
    client.close();
    system.terminate();
    // #ws-standalone-with-config
  }
}
