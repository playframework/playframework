/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import java.util.concurrent.CompletionStage;

import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * Cache another action.
 */
public class CachedAction extends Action<Cached> {

    private AsyncCacheApi cacheApi;

    @Inject
    public CachedAction(AsyncCacheApi cacheApi) {
        this.cacheApi = cacheApi;
    }

    public CompletionStage<Result> call(Request req) {
        final String key = configuration.key();
        final Integer duration = configuration.duration();
        return cacheApi.getOrElseUpdate(key, () -> delegate.call(req), duration);
    }

}
