/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.it.http.ActionCompositionOrderTest.ContextArgsGet;
import play.it.http.ActionCompositionOrderTest.ContextArgsSet;
import play.it.http.ActionCompositionOrderTest.NoopUsingRequest;
import play.mvc.Result;
import play.mvc.Results;

public class PreserveContextArgsController extends MockController {

    @ContextArgsSet
    @NoopUsingRequest
    @ContextArgsGet
    public Result action() {
        return Results.ok();
    }

}
