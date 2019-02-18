/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.cache.inject;
// #inject
import play.cache.*;
import play.mvc.*;

import javax.inject.Inject;

public class Application extends Controller {

  private AsyncCacheApi cache;

  @Inject
  public Application(AsyncCacheApi cache) {
    this.cache = cache;
  }

  // ...
}
// #inject
