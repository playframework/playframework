/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.ws;

//#ws-standalone
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClientConfig;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClient;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;
import play.libs.ws.*;
import play.libs.ws.ahc.*;

import java.util.Optional;

public class Standalone {

    public static void main(String[] args) {
        AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder()
                .setMaxRequestRetry(0)
                .setShutdownQuietPeriod(0)
                .setShutdownTimeout(0).build();

        String name = "wsclient";
        ActorSystem system = ActorSystem.create(name);
        ActorMaterializerSettings settings = ActorMaterializerSettings.create(system);
        ActorMaterializer materializer = ActorMaterializer.create(settings, system, name);

        WSClient client = new AhcWSClient(new DefaultAsyncHttpClient(config), materializer);
        client.url("http://www.google.com").get().whenComplete((r, e) -> {
            Optional.ofNullable(r).ifPresent(response -> {
                String statusText = response.getStatusText();
                System.out.println("Got a response " + statusText);
            });
        }).thenRun(() -> {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRun(system::terminate);

    }
}
//#ws-standalone
