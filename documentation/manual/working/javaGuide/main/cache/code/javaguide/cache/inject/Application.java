/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.cache.inject;
//#inject
import play.cache.*;
import play.mvc.*;

import javax.inject.Inject;

public class Application extends Controller {

    @Inject CacheApi cache;

    // ...
}
//#inject
