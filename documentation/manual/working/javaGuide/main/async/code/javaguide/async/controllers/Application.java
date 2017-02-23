/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.async.controllers;

import play.mvc.Result;
import play.mvc.Controller;
//#async-explicit-ec-imports
import play.libs.concurrent.HttpExecution;
import java.util.concurrent.Executor;
//#async-explicit-ec-imports

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Application extends Controller {
    //#async
    public CompletionStage<Result> index() {
        return CompletableFuture.supplyAsync(() -> intensiveComputation())
                .thenApply(i -> ok("Got result: " + i));
    }
    //#async

    private Executor myThreadPool = null;

    //#async-explicit-ec
    public CompletionStage<Result> index2() {
        // Wrap an existing thread pool, using the context from the current thread
        Executor myEc = HttpExecution.fromThread(myThreadPool);
        return CompletableFuture.supplyAsync(() -> intensiveComputation(), myEc)
                .thenApplyAsync(i -> ok("Got result: " + i), myEc);
    }
    //#async-explicit-ec

    public int intensiveComputation() { return 2;}
}
