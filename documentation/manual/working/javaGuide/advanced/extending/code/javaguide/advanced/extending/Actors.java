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
    private final Application app;

    private ActorRef myActor;

    @Inject
    public Actors(Application app) {
        this.app = app;
    }

    public void onStart() {
        myActor = Akka.system().actorOf(MyActor.props(), "my-actor");
    }

    public static ActorRef getMyActor() {
        return Play.application().plugin(Actors.class).myActor;
    }
}
//#code
