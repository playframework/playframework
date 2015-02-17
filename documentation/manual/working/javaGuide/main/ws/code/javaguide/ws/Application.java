/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.ws;

// #ws-controller
import javax.inject.Inject;

import play.mvc.*;
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;

public class Application extends Controller {

    @Inject WSClient ws;

    // ...
}
// #ws-controller
