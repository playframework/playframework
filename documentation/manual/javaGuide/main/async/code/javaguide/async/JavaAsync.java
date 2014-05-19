/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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
        Promise<Result> promiseOfResult = promiseOfPIValue.map(
          new Function<Double,Result>() {
            public Result apply(Double pi) {
              return ok("PI value computed: " + pi);
            }
          }
        );
        //#promise-pi
        assertThat(status(promiseOfResult.get(1000)), equalTo(200));
    }

    @Test
    public void promiseAsync() {
        //#promise-async
        Promise<Integer> promiseOfInt = Promise.promise(
          new Function0<Integer>() {
            public Integer apply() {
              return intensiveComputation();
            }
          }
        );
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
