/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

/**
 * A state machine with a non-blocking mutex protecting its state.
 */
private[play] class StateMachine[S](initialState: S) {

  /**
   * The current state. Modifications to the state should be performed
   * inside the body of a call to `exclusive`. To read the state, it is
   * usually OK to read this field directly, even though its not volatile
   * or atomic, so long as you're happy about happens-before relationships.
   */
  var state: S = initialState

  val mutex = new NonBlockingMutex()

  /**
   * Exclusive access to the state. The state is read and passed to
   * f. Inside f it is safe to modify the state, if desired.
   */
  def exclusive(f: S => Unit) = mutex.exclusive { f(state) }

}
