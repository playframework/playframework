/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import play.api.GlobalSettings;
import play.api.mvc.Handler;
import play.api.mvc.RequestHeader;
import play.core.j.JavaGlobalSettingsAdapter;
import play.core.j.RequestHeaderImpl;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import scala.Tuple2;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

/**
 * Request handler that delegates to GlobalSettings.
 *
 * @deprecated GlobalSettings is deprecated. Extend DefaultHttpRequestHandler instead.
 */
@Deprecated
@Singleton
public class GlobalSettingsHttpRequestHandler implements HttpRequestHandler {

    private final GlobalSettings global;

    @Inject
    public GlobalSettingsHttpRequestHandler(GlobalSettings global) {
        super();
        this.global = global;
    }

    @Override
    public HandlerForRequest handlerForRequest(Http.RequestHeader request) {
        Tuple2<RequestHeader, Handler> result = global.onRequestReceived(request._underlyingHeader());
        return new HandlerForRequest(new RequestHeaderImpl(result._1()), result._2());
    }

    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
        if (global instanceof JavaGlobalSettingsAdapter) {
            return ((JavaGlobalSettingsAdapter) global).underlying().onRequest(request, actionMethod);
        } else {
            return new Action.Simple() {
                @Override
                public CompletionStage<Result> call(Http.Context ctx) {
                    return delegate.call(ctx);
                }
            };
        }
    }
}
