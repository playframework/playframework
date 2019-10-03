/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.akka;

import akka.actor.typed.Behavior;

import javax.inject.Provider;

// Must be an abstract class instead of an interface for TypedAkka#messageTypeOf to be able to
// extract the type of the parameterized type
public abstract class BehaviorProvider<Message> implements Provider<Behavior<Message>> {}
