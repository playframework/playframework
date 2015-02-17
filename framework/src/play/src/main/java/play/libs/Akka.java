/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import akka.actor.ActorSystem;

import play.api.*;

/**
 * Helper to access the application defined Akka Actor system.
 */
public class Akka {

    /**
     * Retrieve the application Akka Actor system.
     */
    public static ActorSystem system() {
        return play.api.libs.concurrent.Akka.system(Play.current());
    }

}
