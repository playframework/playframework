/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.cache.qualified;

//#qualified
import play.cache.*;
import play.mvc.*;

import javax.inject.Inject;

public class Application extends Controller {

    @Inject @NamedCache("session-cache") CacheApi cache;

    // ...
}
//#qualified
