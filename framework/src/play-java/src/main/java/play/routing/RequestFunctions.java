/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import play.libs.F;
import play.mvc.Http;

import java.util.function.Function;

/**
 * Define functions to be used with {@link RoutingDsl}. The functions here always declared the first parameter as an
 * {@link Http.Request} so that the blocks have access to the request made.
 */
public class RequestFunctions {

    /**
     * This is used to "tag" the functions which requires a request to execute.
     */
    public interface RequestFunction {}

    /**
     * A function that receives a {@link Http.Request}, no parameters, and return a result type. Results are typically
     * {@link play.mvc.Result} or a {@link java.util.concurrent.CompletionStage} that produces a Result.
     *
     * @param <R> the result type.
     */
    public interface Params0<R> extends Function<Http.Request, R>, RequestFunction {}

    /**
     * A function that receives a {@link Http.Request}, a single parameter, and return a result type. Results are typically
     * {@link play.mvc.Result} or a {@link java.util.concurrent.CompletionStage} that produces a Result.
     *
     * @param <P> the parameter type.
     * @param <R> the result type.
     */
    public interface Params1<P, R> extends java.util.function.BiFunction<Http.Request, P, R>, RequestFunction {}

    /**
     * A function that receives a {@link Http.Request}, two parameters, and return a result type. Results are typically
     * {@link play.mvc.Result} or a {@link java.util.concurrent.CompletionStage} that produces a Result.
     *
     * @param <P1> the first parameter type.
     * @param <P2> the second parameter type.
     * @param <R> the result type.
     */
    public interface Params2<P1, P2, R> extends F.Function3<Http.Request, P1, P2, R>, RequestFunction {}

    /**
     * A function that receives a {@link Http.Request}, three parameters, and return a result type. Results are typically
     * {@link play.mvc.Result} or a {@link java.util.concurrent.CompletionStage} that produces a Result.
     *
     * @param <P1> the first parameter type.
     * @param <P2> the second parameter type.
     * @param <P3> the third parameter type.
     * @param <R> the result type.
     */
    public interface Params3<P1, P2, P3, R> extends F.Function4<Http.Request, P1, P2, P3, R>, RequestFunction {}
}
