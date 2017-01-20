/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.ws;

//#ws-standalone-with-config
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.api.libs.ws.WSConfigParser;
import play.api.libs.ws.ahc.AhcConfigBuilder;
import play.api.libs.ws.ahc.AhcWSClientConfig;
import play.api.libs.ws.ahc.AhcWSClientConfigFactory;
import play.libs.ws.WSClient;
import play.libs.ws.ahc.AhcWSClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;

public class StandaloneWithConfig {

    public static void main(String[] args) {
        Config conf = ConfigFactory.load();

        String name = "wsclient";
        ActorSystem system = ActorSystem.create(name);
        ActorMaterializerSettings settings = ActorMaterializerSettings.create(system);
        ActorMaterializer materializer = ActorMaterializer.create(settings, system, name);

        WSConfigParser parser = new WSConfigParser(conf, ClassLoader.getSystemClassLoader());
        AhcWSClientConfig clientConf = AhcWSClientConfigFactory.forClientConfig(parser.parse());

        final DefaultAsyncHttpClientConfig asyncHttpClientConfig = new AhcConfigBuilder(clientConf).configure().build();
        WSClient client = new AhcWSClient(new DefaultAsyncHttpClient(asyncHttpClientConfig),
                materializer);
    }
}
//#ws-standalone-with-config
