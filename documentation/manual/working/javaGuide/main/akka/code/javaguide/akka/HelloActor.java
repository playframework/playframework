/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
//#actor
//###replace: package actors;
package javaguide.akka;

import akka.actor.*;
//###replace: import actors.HelloActorProtocol.*;
import javaguide.akka.HelloActorProtocol.*;

public class HelloActor extends UntypedActor {

    public static Props props = Props.create(HelloActor.class);

    public void onReceive(Object msg) throws Exception {
        if (msg instanceof SayHello) {
            sender().tell("Hello, " + ((SayHello) msg).name, self());
        }
    }
}
//#actor
