/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cache;

import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;

/** Cache another action. */
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
