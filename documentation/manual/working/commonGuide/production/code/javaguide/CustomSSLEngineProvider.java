/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide;

// #javaexample
import play.server.ApplicationProvider;
import play.server.SSLEngineProvider;

import javax.net.ssl.*;
import java.security.NoSuchAlgorithmException;

public class CustomSSLEngineProvider implements SSLEngineProvider {
  private ApplicationProvider applicationProvider;

  public CustomSSLEngineProvider(ApplicationProvider applicationProvider) {
    this.applicationProvider = applicationProvider;
  }

  @Override
  public SSLEngine createSSLEngine() {
    try {
      // change it to your custom implementation
      return SSLContext.getDefault().createSSLEngine();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
// #javaexample
