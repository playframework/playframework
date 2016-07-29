/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Provides utility functions to work with resources.
 */

public class Resources {

    public static <T extends AutoCloseable, U> CompletionStage<U> asyncTryWithResource(
            T resource, Function<T, CompletionStage<U>> body
    ) {
        try {
            CompletionStage<U> completionStage = body.apply(resource);
            completionStage.whenCompleteAsync((u, throwable) -> tryCloseResource(resource));
            return completionStage;
        } catch (RuntimeException e) {
            tryCloseResource(resource);
            throw e;
        } catch (Exception e) {
            tryCloseResource(resource);
            throw new RuntimeException(e);
        }
    }

    private static <T extends AutoCloseable> void tryCloseResource(T resource) {
        try {
            resource.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
