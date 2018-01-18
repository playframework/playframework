/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.cache.inject;
//#inject
import play.cache.*;
import play.mvc.*;

import javax.inject.Inject;

public class Application extends BaseController {

    private AsyncCacheApi cache;

    @Inject
    public Application(AsyncCacheApi cache) {
        this.cache = cache;
    }

    // ...
}
//#inject
