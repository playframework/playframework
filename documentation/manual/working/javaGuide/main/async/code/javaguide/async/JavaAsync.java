/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.async;

import org.junit.Test;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.Result;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static play.mvc.Results.ok;
import static play.test.Helpers.*;

public class JavaAsync {

    @Test
    public void promisePi() {
        //#promise-pi
        Promise<Double> promiseOfPIValue = computePIAsynchronously();
        Promise<Result> promiseOfResult = promiseOfPIValue.map(pi ->
                        ok("PI value computed: " + pi)
        );
        //#promise-pi
        assertThat(promiseOfResult.get(1000).status(), equalTo(200));
    }

    @Test
    public void promiseAsync() {
        //#promise-async
        Promise<Integer> promiseOfInt = Promise.promise(() -> intensiveComputation());
        //#promise-async
        assertEquals(intensiveComputation(), promiseOfInt.get(1000));
    }

    private static Promise<Double> computePIAsynchronously() {
        return Promise.pure(Math.PI);
    }

    private static Integer intensiveComputation() {
        return 1 + 1;
    }

}
