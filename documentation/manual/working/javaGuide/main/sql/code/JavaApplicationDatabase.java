/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.sql;

import javax.inject.Inject;

import play.mvc.*;
import play.db.*;

class JavaApplicationDatabase extends Controller {
    @Inject Database db;
    // ...
}
