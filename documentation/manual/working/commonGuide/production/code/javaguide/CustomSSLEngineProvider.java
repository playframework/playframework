/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide;

// #javaexample
import play.server.ApplicationProvider;
import play.server.SSLEngineProvider;

import javax.inject.Inject;
import javax.net.ssl.*;
import java.security.NoSuchAlgorithmException;

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
