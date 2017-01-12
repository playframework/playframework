/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.libs.concurrent;

import akka.stream.Supervision;

import java.util.function.Function;

public interface PlaySupervisionProvider {
    Function<Throwable, Supervision.Directive> decider();
}
