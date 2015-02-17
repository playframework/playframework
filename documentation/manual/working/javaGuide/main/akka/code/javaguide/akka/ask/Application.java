/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package javaguide.akka.ask;

import javaguide.akka.HelloActor;
import javaguide.akka.HelloActorProtocol.SayHello;

//#ask
import akka.actor.*;
import play.mvc.*;
import play.libs.F.*;
import javax.inject.*;

import static akka.pattern.Patterns.ask;

@Singleton
public class Application extends Controller {

    final ActorRef helloActor;

    @Inject public Application(ActorSystem system) {
        helloActor = system.actorOf(HelloActor.props);
    }

    public Promise<Result> sayHello(String name) {
        return Promise.wrap(ask(helloActor, new SayHello(name), 1000))
                .map(response -> ok((String) response));
    }
}
//#ask
