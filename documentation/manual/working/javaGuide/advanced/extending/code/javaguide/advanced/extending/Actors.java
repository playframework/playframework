/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
//#code
//###replace: package actors;
package javaguide.advanced.extending;

import akka.actor.*;
import play.*;
import play.libs.Akka;

import javax.inject.Inject;

public class Actors extends Plugin {

    private @Inject ActorSystem system;

    private ActorRef myActor;

    public void onStart() {
        myActor = system.actorOf(MyActor.props(), "my-actor");
    }

    public static ActorRef getMyActor() {
        return Play.application().plugin(Actors.class).myActor;
    }
}
//#code
