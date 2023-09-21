/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsingdeferred;

import static play.it.http.parsingdeferred.DeferredBodyParsingSpec.buildActionCompositionMessage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

@With(SimpleActionAnnotationAction.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleActionAnnotation {}

class SimpleActionAnnotationAction extends Action<SimpleActionAnnotation> {
  @Override
  public CompletionStage<Result> call(Http.Request req) {
    return delegate.call(
        req.addAttr(Attrs.REQUEST_FLOW, buildActionCompositionMessage(req.asScala())));
  }
}
