/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.ws;

// #ws-controller
import javax.inject.Inject;

import play.mvc.*;
import play.libs.ws.*;
import java.util.concurrent.CompletionStage;

public class Application extends Controller {

    @Inject WSClient ws;

    // ...
}
// #ws-controller
