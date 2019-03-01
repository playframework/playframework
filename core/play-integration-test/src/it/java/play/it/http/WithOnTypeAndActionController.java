/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import play.it.http.ActionCompositionOrderTest.FirstAction;
import play.it.http.ActionCompositionOrderTest.SecondAction;

@With({FirstAction.class, SecondAction.class})
public class WithOnTypeAndActionController extends MockController {

    @With({FirstAction.class, SecondAction.class})
    public Result action() {
        return Results.ok();
    }

}
