/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import play.api.libs.concurrent.StateMachine
import play.api.libs.iteratee._
import scala.concurrent.Promise

private[streams] object IterateeSubscriber {
  /**
   * Internal state of the Subscriber.
   *
   * @tparam T type of elements.
   * @tparam R The result type of the Iteratee.
   */
  sealed trait State[T, R]
  /**
   * A Subscriber that hasn't had onSubscribe called on it yet, and whose
   * Iteratee hasn't resolved to a Step yet. This is the initial state of the
   * Subscriber.
   *
   * @param result A Promise of the eventual result of this Subscriber.
   */
  case class NotSubscribedNoStep[T, R](result: Promise[Iteratee[T, R]]) extends State[T, R]
  /**
   * A Subscriber that has had onSubscribe called on it but
   * doesn't yet know the current Step of its Iteratee.
   *
   * @param subs The Subscription the Subscriber is subscribed to.
   * @param result A Promise of the eventual result of this Subscriber.
   */
  case class SubscribedNoStep[T, R](subs: Subscription, result: Promise[Iteratee[T, R]]) extends State[T, R]
  /**
   * A Subscriber that hasn't had onSubscribe called on it yet, but whose
   * Iteratee is known to have a Step of Cont.
   *
   * @param cont The current Step of the Iteratee.
   * @param result A Promise of the eventual result of this Subscriber.
   */
  case class NotSubscribedWithCont[T, R](cont: Step.Cont[T, R], result: Promise[Iteratee[T, R]]) extends State[T, R]
  /**
   * A Subscriber that has had onSubscribe called, has a current Iteratee with
   * a Step of Cont and is currently waiting for an element that it has
   * requested from the Subscription.
   *
   * @param subs The Subscription the Subscriber is subscribed to.
   * @param cont The current Step of the Iteratee.
   * @param result A Promise of the eventual result of this Subscriber.
   */
  case class SubscribedWithCont[T, R](subs: Subscription, cont: Step.Cont[T, R], result: Promise[Iteratee[T, R]]) extends State[T, R]
  /**
   * A Subscriber that has been completed with onComplete but whose Iteratee
   * hasn't resolved ot a Step yet. If the Iteratee resolves to Cont then it
   * Input.EOF will be fed to it.
   *
   * @param result A Promise of the eventual result of this Subscriber.
   */
  case class CompletedNoStep[T, R](result: Promise[Iteratee[T, R]]) extends State[T, R]
  /**
   * A Subscriber that is finished. We don't track the precise reason for
   * being finished.
   *
   * @param resultIteratee The result of this Subscriber.
   */
  case class Finished[T, R](resultIteratee: Iteratee[T, R]) extends State[T, R]
}

import IterateeSubscriber._

private[streams] class IterateeSubscriber[T, R, S](iter0: Iteratee[T, R])
    extends StateMachine[State[T, R]](initialState = NotSubscribedNoStep(Promise[Iteratee[T, R]]())) with Subscriber[T] {

  // We immediately fold on the iteratee
  getNextStepFromIteratee(iter0)

  /**
   * The final result of this Iteratee, either a Done or Error Iteratee,
   * or the last Iteratee when the Subscription is completed.
   */
  def result: Iteratee[T, R] = state match {
    case NotSubscribedNoStep(result) =>
      promiseToIteratee(result)
    case SubscribedNoStep(subs, result) =>
      promiseToIteratee(result)
    case NotSubscribedWithCont(cont, result) =>
      promiseToIteratee(result)
    case SubscribedWithCont(subs, cont, result) =>
      promiseToIteratee(result)
    case CompletedNoStep(result) =>
      promiseToIteratee(result)
    case Finished(resultIteratee) =>
      resultIteratee
  }

  // Streams methods

  override def onSubscribe(subs: Subscription): Unit = exclusive {
    case NotSubscribedNoStep(result) =>
      state = SubscribedNoStep(subs, result)
    case SubscribedNoStep(subs, result) =>
      throw new IllegalStateException("Can't subscribe twice")
    case NotSubscribedWithCont(cont, result) =>
      subs.request(1)
      state = SubscribedWithCont(subs, cont, result)
    case SubscribedWithCont(subs, cont, result) =>
      throw new IllegalStateException("Can't subscribe twice")
    case CompletedNoStep(result) =>
      throw new IllegalStateException("Can't subscribe once completed")
    case Finished(resultIteratee) =>
      subs.cancel()
  }

  override def onComplete(): Unit = exclusive {
    case NotSubscribedNoStep(result) =>
      state = CompletedNoStep(result)
    case SubscribedNoStep(subs, result) =>
      state = CompletedNoStep(result)
    case NotSubscribedWithCont(cont, result) =>
      finishWithCompletedCont(cont, result)
    case SubscribedWithCont(subs, cont, result) =>
      finishWithCompletedCont(cont, result)
    case CompletedNoStep(result) =>
      throw new IllegalStateException("Can't complete twice")
    case Finished(resultIteratee) =>
      ()
  }

  override def onError(cause: Throwable): Unit = exclusive {
    case NotSubscribedNoStep(result) =>
      finishWithError(cause, result)
    case SubscribedNoStep(subs, result) =>
      finishWithError(cause, result)
    case NotSubscribedWithCont(cont, result) =>
      finishWithError(cause, result)
    case SubscribedWithCont(subs, cont, result) =>
      finishWithError(cause, result)
    case CompletedNoStep(result) =>
      throw new IllegalStateException("Can't receive error once completed")
    case Finished(resultIteratee) =>
      ()
  }

  override def onNext(element: T): Unit = exclusive {
    case NotSubscribedNoStep(result) =>
      throw new IllegalStateException("Got next element before subscribed")
    case SubscribedNoStep(subs, result) =>
      throw new IllegalStateException("Got next element before requested")
    case NotSubscribedWithCont(cont, result) =>
      throw new IllegalStateException("Got next element before subscribed")
    case SubscribedWithCont(subs, cont, result) =>
      continueWithNext(subs, cont, element, result)
    case CompletedNoStep(result) =>
      throw new IllegalStateException("Can't receive error once completed")
    case Finished(resultIteratee) =>
      ()
  }

  private def continueWithNext(subs: Subscription, cont: Step.Cont[T, R], element: T, result: Promise[Iteratee[T, R]]): Unit = {
    val nextIteratee = cont.k(Input.El(element))
    getNextStepFromIteratee(nextIteratee)
    state = SubscribedNoStep(subs, result)
  }

  /**
   * Called when the iteratee folds to a Cont step. We may want to feed
   * an Input to the Iteratee.
   */
  private def onContStep(cont: Step.Cont[T, R]): Unit = {
    exclusive {
      case NotSubscribedNoStep(result) =>
        state = NotSubscribedWithCont(cont, result)
      case SubscribedNoStep(subs, result) =>
        subs.request(1)
        state = SubscribedWithCont(subs, cont, result)
      case NotSubscribedWithCont(cont, result) =>
        throw new IllegalStateException("Can't get cont twice")
      case SubscribedWithCont(subs, cont, result) =>
        throw new IllegalStateException("Can't get cont twice")
      case CompletedNoStep(result) =>
        finishWithCompletedCont(cont, result)
      case Finished(resultIteratee) =>
        ()
    }
  }

  /**
   * Called when the iteratee folds to a Done or Error step. We may want to
   * cancel our Subscription.
   */
  private def onDoneOrErrorStep(doneOrError: Step[T, R]): Unit = exclusive {
    case NotSubscribedNoStep(result) =>
      finishWithDoneOrErrorStep(doneOrError, result)
    case SubscribedNoStep(subs, result) =>
      subs.cancel()
      finishWithDoneOrErrorStep(doneOrError, result)
    case NotSubscribedWithCont(cont, result) =>
      throw new IllegalStateException("Can't get done or error while has cont")
    case SubscribedWithCont(subs, cont, result) =>
      throw new IllegalStateException("Can't get done or error while has cont")
    case CompletedNoStep(result) =>
      finishWithDoneOrErrorStep(doneOrError, result)
    case Finished(resultIteratee) =>
      ()
  }

  /**
   * Folds an Iteratee to get its Step. The Step is used to choose a method
   * to call.
   */
  private def getNextStepFromIteratee(iter: Iteratee[T, R]): Unit = {
    iter.pureFold {
      case c @ Step.Cont(_) => onContStep(c)
      case d @ Step.Done(_, _) => onDoneOrErrorStep(d)
      case e @ Step.Error(_, _) => onDoneOrErrorStep(e)
    }(Execution.trampoline)
  }

  /** Flattens a Promise[Iteratee] to an Iteratee. */
  private def promiseToIteratee(result: Promise[Iteratee[T, R]]) = Iteratee.flatten(result.future)

  /**
   * Finishes the Subscription when the Step is Cont and onComplete
   * has been called. This is done by feeding EOF to the Cont Iteratee.
   */
  private def finishWithCompletedCont(cont: Step.Cont[T, R], result: Promise[Iteratee[T, R]]): Unit = {
    val nextIteratee = cont.k(Input.EOF)
    result.success(nextIteratee)
    state = Finished(nextIteratee)
  }

  /**
   * Finishes the Subscription when onError has been called. This is done by
   * setting the Iteratee Future to an failed state.
   */
  private def finishWithError(cause: Throwable, result: Promise[Iteratee[T, R]]): Unit = {
    result.failure(cause)
    state = Finished(promiseToIteratee(result))
  }

  /**
   * Finishes the Subscription when the Step is Done or Error. This is done by
   * setting the result to the Step's iteratee.
   */
  private def finishWithDoneOrErrorStep(step: Step[T, R], result: Promise[Iteratee[T, R]]): Unit = {
    val nextIteratee = step.it
    result.success(nextIteratee)
    state = Finished(nextIteratee)
  }

}
