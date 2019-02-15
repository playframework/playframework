/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
            return completionStage.whenComplete((u, throwable) -> tryCloseResource(resource));
        } catch (RuntimeException e) {
            tryCloseResource(resource);
            throw e;
        } catch (Exception e) {
            tryCloseResource(resource);
            throw new RuntimeException("Error trying with resource", e);
        }
    }

    private static <T extends AutoCloseable> void tryCloseResource(T resource) {
        try {
            resource.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error closing resource", e);
        }
    }
}
