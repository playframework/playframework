/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ehcache.inject;
// #inject
import javax.inject.Inject;
import play.cache.*;
import play.mvc.*;

public class Application extends Controller {

  private AsyncCacheApi cache;

  @Inject
  public Application(AsyncCacheApi cache) {
    this.cache = cache;
  }

  // ...
}
// #inject
