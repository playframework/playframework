/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.extending;

import java.io.IOException;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

public class MyWSClient implements WSClient {
  @Override
  public Object getUnderlying() {
    return null;
  }

  @Override
  public play.api.libs.ws.WSClient asScala() {
    return null;
  }

  @Override
  public WSRequest url(String url) {
    return null;
  }

  @Override
  public void close() throws IOException {}
}
