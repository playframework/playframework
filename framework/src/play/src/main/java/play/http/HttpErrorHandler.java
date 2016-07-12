/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;


import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import scala.Enumeration;
import scala.Function1;
import scala.Option;
import scala.Tuple3;

import java.util.concurrent.CompletionStage;

/**
 * Component for handling HTTP errors in Play.
 *
 * @since 2.4.0
 */
public interface HttpErrorHandler {

    /**
     * Invoked when a error occurs.
     *
     * @param error The error.
     */
    CompletionStage<Result> onError(HttpError<?> error);

}
