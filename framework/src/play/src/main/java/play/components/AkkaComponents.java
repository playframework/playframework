/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.components;

import akka.actor.ActorSystem;
import akka.stream.Materializer;
import scala.concurrent.ExecutionContext;

/**
 * Akka and Akka Streams components.
 */
public interface AkkaComponents {

    ActorSystem actorSystem();

    Materializer materializer();

    ExecutionContext executionContext();

}
