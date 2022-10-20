/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.cache.qualified;

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
