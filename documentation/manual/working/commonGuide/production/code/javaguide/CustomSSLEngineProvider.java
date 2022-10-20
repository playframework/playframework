/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide;

// #javaexample
import java.security.NoSuchAlgorithmException;
import javax.inject.Inject;
import javax.net.ssl.*;
import play.server.ApplicationProvider;
import play.server.SSLEngineProvider;

public class CustomSSLEngineProvider implements SSLEngineProvider {

  private final ApplicationProvider applicationProvider;

  @Inject
  public CustomSSLEngineProvider(ApplicationProvider applicationProvider) {
    this.applicationProvider = applicationProvider;
  }

  @Override
  public SSLEngine createSSLEngine() {
    return sslContext().createSSLEngine();
  }

  @Override
  public SSLContext sslContext() {
    try {
      // Change it to your custom implementation, possibly using ApplicationProvider.
      return SSLContext.getDefault();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
// #javaexample
