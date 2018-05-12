/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc;

import java.util.function.Function;

import akka.util.ByteString;
import play.api.mvc.Handler;
import play.core.Execution;
import play.libs.streams.Accumulator;
import play.mvc.Http.RequestHeader;
import scala.runtime.AbstractFunction1;

/**
 * Given a `RequestHeader`, an `EssentialAction` consumes the request body (a `ByteString`) and returns a `Result`.
 *
 * An `EssentialAction` is a `Handler`, which means it is one of the objects that Play uses to handle requests. You
 * can use this to create your action inside a filter, for example.
 *
 * Unlike traditional method-based Java actions, EssentialAction does not use a context.
 */
public abstract class EssentialAction
    extends AbstractFunction1<play.api.mvc.RequestHeader, play.api.libs.streams.Accumulator<ByteString, play.api.mvc.Result>>
    implements play.api.mvc.EssentialAction, Handler {

    public static EssentialAction of(Function<RequestHeader, Accumulator<ByteString, Result>> action) {
        return new EssentialAction() {
          @Override
          public Accumulator<ByteString, Result> apply(RequestHeader requestHeader) {
              return action.apply(requestHeader);
          }
        };
    }

    public abstract Accumulator<ByteString, Result> apply(RequestHeader requestHeader);

    @Override
    public play.api.libs.streams.Accumulator<ByteString, play.api.mvc.Result> apply(play.api.mvc.RequestHeader rh) {
        return apply(rh.asJava())
            .map(Result::asScala, Execution.trampoline())
            .asScala();
    }

    @Override
    public play.api.mvc.EssentialAction apply() {
        return this;
    }

    @Override
    public EssentialAction asJava() {
        return this;
    }
}
