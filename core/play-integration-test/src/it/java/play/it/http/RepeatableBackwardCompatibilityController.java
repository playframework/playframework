/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http;

import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import play.it.http.ActionCompositionOrderTest.SomeActionAnnotation;
import play.it.http.ActionCompositionOrderTest.SomeRepeatable;

/**
 * Checks backward compatibility:
 * Here only SomeActionAnnotation should run but the inner actions should NOT.
 * We always check first if an outer annotation has @With defined before trying to unwrap it to see if it may is a container annotation.
 * If SomeActionAnnotation below would not define @With it would be seen as container annotation and the the wrapped annotations would run -
 * but also just because the inner annotations have @Repeatable defined; if they wouldn't be defined @Repeatable then they wouldn't run as well.
 */
public class RepeatableBackwardCompatibilityController extends MockController {

    @SomeActionAnnotation({ // -> defines @With and therefore is NOT seen as container annotation
        @SomeRepeatable, // -> is defined @Repeatable and also has @With so this could be an actual action annotation that could run
        @SomeRepeatable
    })
    public Result action() {
        return Results.ok();
    }
}
