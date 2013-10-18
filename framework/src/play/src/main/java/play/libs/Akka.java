/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.libs;

import akka.actor.ActorSystem;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import play.api.*;
import play.core.j.FPromiseHelper;
import play.libs.F.*;

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
