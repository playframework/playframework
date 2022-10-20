/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.advanced.extending;

import play.libs.ws.WSClient;

public class MyWSClientProvider implements javax.inject.Provider<WSClient> {
  @Override
  public WSClient get() {
    return new MyWSClient();
  }
}
