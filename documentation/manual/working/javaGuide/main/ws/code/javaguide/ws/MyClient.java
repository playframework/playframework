/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.ws;

// #ws-controller
import javax.inject.Inject;

import play.mvc.*;
import play.libs.ws.*;
import java.util.concurrent.CompletionStage;

public class MyClient implements WSBodyReadables, WSBodyWritables {
    private final WSClient ws;

    @Inject
    public MyClient(WSClient ws) {
        this.ws = ws;
    }
    // ...
}
// #ws-controller
