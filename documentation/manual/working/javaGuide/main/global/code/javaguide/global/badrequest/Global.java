/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.global.badrequest;

//#global
import play.*;
import play.mvc.*;
import play.mvc.Http.*;
import play.libs.F.*;

import static play.mvc.Results.*;

public class Global extends GlobalSettings {

    public Promise<Result> onBadRequest(RequestHeader request, String error) {
        return Promise.<Result>pure(badRequest("Don't try to hack the URI!"));
    }

}
//#global

