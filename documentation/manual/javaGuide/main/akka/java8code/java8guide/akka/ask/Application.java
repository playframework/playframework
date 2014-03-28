/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package java8guide.akka.ask;

//#ask
import akka.actor.*;
import play.mvc.*;
import play.libs.Akka;
import play.libs.F.Promise;

import static akka.pattern.Patterns.ask;

public class Application extends Controller {

    public static Promise<Result> index() {
        ActorSelection actor = Akka.system().actorSelection("user/my-actor");
        return Promise.wrap(ask(actor, "hello", 1000))
                      .map(response -> ok(response.toString()));
    }
}
//#ask
