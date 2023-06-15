/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import org.apache.pekko.stream.Materializer;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import play.mvc.Http.RequestHeader;
import scala.Function1;
import scala.concurrent.Future;
import scala.jdk.javaapi.FutureConverters;

public abstract class Filter extends EssentialFilter {

  protected final Materializer materializer;

  public Filter(Materializer mat) {
    super();
    this.materializer = mat;
  }

  public abstract CompletionStage<Result> apply(
      Function<RequestHeader, CompletionStage<Result>> next, RequestHeader rh);

  @Override
  public EssentialAction apply(EssentialAction next) {
    return asScala().apply(next).asJava();
  }

  public play.api.mvc.Filter asScala() {
    return new play.api.mvc.Filter() {
      @Override
      public Future<play.api.mvc.Result> apply(
          Function1<play.api.mvc.RequestHeader, Future<play.api.mvc.Result>> next,
          play.api.mvc.RequestHeader requestHeader) {
        return FutureConverters.asScala(
            Filter.this
                .apply(
                    (rh) ->
                        FutureConverters.asJava(next.apply(rh.asScala()))
                            .thenApply(play.api.mvc.Result::asJava),
                    requestHeader.asJava())
                .thenApply(Result::asScala));
      }

      @Override
      public Materializer mat() {
        return materializer;
      }
    };
  }
}
