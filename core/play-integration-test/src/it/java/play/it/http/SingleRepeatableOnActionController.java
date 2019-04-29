/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.Result;
import play.mvc.Results;

import play.it.http.ActionCompositionOrderTest.SomeRepeatable;

public class SingleRepeatableOnActionController extends MockController {

    @SomeRepeatable // runs two actions
    public Result action() {
        return Results.ok();
    }

}
