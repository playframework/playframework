/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.Result;
import play.mvc.Results;

import play.it.http.ActionCompositionOrderTest.SomeRepeatable;

@SomeRepeatable // runs two actions
public class SingleRepeatableOnTypeAndActionController extends MockController {

    @SomeRepeatable // again runs two actions
    public Result action() {
        return Results.ok();
    }

}
