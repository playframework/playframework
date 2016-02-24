/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.mvc;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import akka.stream.Materializer;
import play.core.j.RequestHeaderImpl;
import play.mvc.Http.RequestHeader;
import scala.Function1;
import scala.compat.java8.FutureConverters;

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
        return new play.api.mvc.Filter() {
            @Override
            public Materializer mat() {
                return materializer;
            }

            @Override
            public play.api.mvc.EssentialAction apply(play.api.mvc.EssentialAction next) {
                // Manually mix in the implementation from the EssentialAction trait
                return play.api.mvc.Filter$class.apply(this, next);
            }

            @Override
            public scala.concurrent.Future<play.api.mvc.Result> apply(
                    Function1<play.api.mvc.RequestHeader,
                    scala.concurrent.Future<play.api.mvc.Result>> next,
                    play.api.mvc.RequestHeader requestHeader) {
                return FutureConverters.toScala(
                    Filter.this.apply(
                        (rh) -> FutureConverters.toJava(next.apply(rh._underlyingHeader())).thenApply(r -> r.asJava()),
                        new RequestHeaderImpl(requestHeader)
                    ).thenApply(r -> r.asScala())
                );
            }

            @Override
            public EssentialFilter asJava() {
                return Filter.this;
            }
        };
    }
}
