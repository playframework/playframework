/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import akka.stream.Materializer;
import play.core.j.AbstractFilter;
import play.mvc.Http.RequestHeader;
import scala.Function1;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public abstract class Filter extends EssentialFilter {

    protected final Materializer materializer;

    public Filter(Materializer mat) {
        super();
        this.materializer = mat;
    }

    public abstract CompletionStage<Result> apply(Function<RequestHeader, CompletionStage<Result>> next, RequestHeader rh);

    @Override
    public EssentialAction apply(EssentialAction next) {
        return asScala().apply(next).asJava();
    }

    public play.api.mvc.Filter asScala() {
        return new AbstractFilter(materializer, this) {
            @Override
            public Future<play.api.mvc.Result> apply(
                    Function1<play.api.mvc.RequestHeader, Future<play.api.mvc.Result>> next,
                    play.api.mvc.RequestHeader requestHeader) {
                return FutureConverters.toScala(
                        Filter.this.apply(
                                (rh) -> FutureConverters.toJava(next.apply(rh.asScala())).thenApply(play.api.mvc.Result::asJava),
                                requestHeader.asJava()
                        ).thenApply(Result::asScala)
                );
            }
        };
    }
}
