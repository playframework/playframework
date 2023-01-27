/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ehcache.qualified;

// #qualified
import javax.inject.Inject;
import play.cache.*;
import play.mvc.*;

public class Application extends Controller {

  @Inject
  @NamedCache("session-cache")
  SyncCacheApi cache;

  // ...
}
// #qualified
