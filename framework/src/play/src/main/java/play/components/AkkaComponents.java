/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import scala.concurrent.ExecutionContext;

/**
 * Akka and Akka Streams components.
 */
public interface AkkaComponents {

    ActorSystem actorSystem();

    default Materializer materializer() {
        return ActorMaterializer.create(actorSystem());
    }

    default ExecutionContext executionContext() {
        return actorSystem().dispatcher();
    }

}
