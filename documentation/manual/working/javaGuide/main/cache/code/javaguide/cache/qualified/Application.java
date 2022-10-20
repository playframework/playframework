/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.cache.qualified;

// #qualified
import play.cache.*;
import play.mvc.*;

import javax.inject.Inject;

public class Application extends Controller {

  @Inject
  @NamedCache("session-cache")
  SyncCacheApi cache;

  // ...
}
// #qualified
