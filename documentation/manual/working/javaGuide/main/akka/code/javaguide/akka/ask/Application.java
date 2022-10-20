/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.ask;

import javaguide.akka.HelloActor;
import javaguide.akka.HelloActorProtocol.SayHello;

// #ask
import akka.actor.*;
import play.mvc.*;
import scala.jdk.javaapi.FutureConverters;
import javax.inject.*;
import java.util.concurrent.CompletionStage;

import static akka.pattern.Patterns.ask;

@Singleton
public class Application extends Controller {

  final ActorRef helloActor;

  @Inject
  public Application(ActorSystem system) {
    helloActor = system.actorOf(HelloActor.getProps());
  }

  public CompletionStage<Result> sayHello(String name) {
    return FutureConverters.asJava(ask(helloActor, new SayHello(name), 1000))
        .thenApply(response -> ok((String) response));
  }
}
// #ask
