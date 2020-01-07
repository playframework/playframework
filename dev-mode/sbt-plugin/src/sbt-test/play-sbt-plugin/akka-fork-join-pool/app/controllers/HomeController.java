/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

public class HomeController extends Controller {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    public Result index() {
        // Replace digits to not log the thread number
        log.debug(Thread.currentThread().getName().replaceAll("\\d", ""));
        int sum = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel().map(v -> {
            log.debug(Thread.currentThread().getName().replaceAll("\\d", ""));
            return v;
        }).sum();
        return ok("Sum: " + sum);
    }
}
