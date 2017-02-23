/*
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package javaguide.sql;

import javax.inject.Inject;

import play.mvc.*;
import play.db.*;

class JavaApplicationDatabase extends Controller {

    private Database db;

    @Inject
    public JavaApplicationDatabase(Database db) {
        this.db = db;
    }

    // ...
}
