/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka.inject;
import javaguide.akka.ConfiguredActorProtocol;

//#inject
import akka.actor.ActorRef;
import play.libs.F;
import play.mvc.*;

import javax.inject.Inject;
import javax.inject.Named;

import static akka.pattern.Patterns.ask;

public class Application extends Controller {

    @Inject @Named("configured-actor")
    ActorRef configuredActor;

    public F.Promise<Result> getConfig() {
        return F.Promise.wrap(ask(configuredActor,
            new ConfiguredActorProtocol.GetConfig(), 1000)
        ).map(response -> ok((String) response));
    }
}
//#inject