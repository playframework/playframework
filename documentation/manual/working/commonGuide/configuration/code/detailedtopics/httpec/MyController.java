/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package detailedtopics.httpec;

//#http-execution-context
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.mvc.*;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class MyController extends Controller {
    @Inject HttpExecutionContext ec;
    @Inject WSClient ws;

    public CompletionStage<Result> index() {
        String checkUrl = request().getQueryString("url");
        return ws.url(checkUrl).get().thenApplyAsync((response) -> {
            session().put("lastStatus", Integer.toString(response.getStatus()));
            return ok();
        }, ec.current());
    }
}
//#http-execution-context
