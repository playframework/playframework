/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
