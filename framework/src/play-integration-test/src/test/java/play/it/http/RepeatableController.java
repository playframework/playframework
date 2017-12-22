/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.http;

import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import play.it.http.ActionCompositionOrderTest.FourthRepeatableAction;
import play.it.http.ActionCompositionOrderTest.SomeRepeatable;
import play.it.http.ActionCompositionOrderTest.ThirdRepeatableAction;

/**
 * We can't use repeatable annotations in Scala right now, that's why this class has to be a Java class.
 * See https://issues.scala-lang.org/browse/SI-9529
 */
@SomeRepeatable // actually runs two actions
@SomeRepeatable // once more, so makes it four
@With({ThirdRepeatableAction.class, FourthRepeatableAction.class }) // run two more
public class RepeatableController extends MockController {

    @SomeRepeatable // again runs two actions
    @SomeRepeatable // plus two more
    @With({FourthRepeatableAction.class, ThirdRepeatableAction.class }) // here we switch the order of these two ;)
    public Result action() {
        return Results.ok();
    }

}
