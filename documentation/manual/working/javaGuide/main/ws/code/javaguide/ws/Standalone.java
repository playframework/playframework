/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws;

// #ws-standalone-imports
import akka.actor.ActorSystem;
import akka.stream.Materializer;

import play.shaded.ahc.org.asynchttpclient.*;
import play.libs.ws.*;
import play.libs.ws.ahc.*;

import org.junit.Test;
// #ws-standalone-imports

import java.util.Optional;

public class Standalone {

  @Test
  public void testMe() {
    // #ws-standalone
    // Set up Akka
    String name = "wsclient";
    ActorSystem system = ActorSystem.create(name);
    Materializer materializer = Materializer.matFromSystem(system);

    // Set up AsyncHttpClient directly from config
    AsyncHttpClientConfig asyncHttpClientConfig =
        new DefaultAsyncHttpClientConfig.Builder()
            .setMaxRequestRetry(0)
            .setShutdownQuietPeriod(0)
            .setShutdownTimeout(0)
            .build();
    AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig);

    // Set up WSClient instance directly from asynchttpclient.
    WSClient client = new AhcWSClient(asyncHttpClient, materializer);

    // Call out to a remote system and then and close the client and akka.
    client
        .url("http://www.google.com")
        .get()
        .whenComplete(
            (r, e) -> {
              Optional.ofNullable(r)
                  .ifPresent(
                      response -> {
                        String statusText = response.getStatusText();
                        System.out.println("Got a response " + statusText);
                      });
            })
        .thenRun(
            () -> {
              try {
                client.close();
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .thenRun(system::terminate);
    // #ws-standalone
  }
}
