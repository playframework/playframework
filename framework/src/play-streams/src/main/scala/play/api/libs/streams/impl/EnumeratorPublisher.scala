package play.api.libs.streams.impl

import org.reactivestreams._
import play.api.libs.concurrent.StateMachine
import play.api.libs.iteratee._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Success, Try }

import scala.language.higherKinds

/**
 * Creates Subscriptions that link Subscribers to an Enumerator.
 */
private[streams] trait EnumeratorSubscriptionFactory[T] extends SubscriptionFactory[T] {

  def enum: Enumerator[T]
  def emptyElement: Option[T]

  override def createSubscription[U >: T](
    subr: Subscriber[U],
    onSubscriptionEnded: SubscriptionHandle[U] => Unit) = {
    new EnumeratorSubscription[T, U](enum, emptyElement, subr, onSubscriptionEnded)
  }

}

/**
 * Adapts an Enumerator to a Publisher.
 */
private[streams] final class EnumeratorPublisher[T](
  val enum: Enumerator[T],
  val emptyElement: Option[T] = None) extends RelaxedPublisher[T] with EnumeratorSubscriptionFactory[T]

private[streams] object EnumeratorSubscription {

  /**
   * Internal state of the Publisher.
   */
  sealed trait State[+T]
  /**
   * An active Subscription with n outstanding requested elements.
   * @param n Elements that have been requested by the Subscriber. May be 0.
   * @param attached The attached Iteratee we're using to read from the
   * Enumerator. Will be Unattached until the first element is requested.
   */
  final case class Requested[T](n: Long, attached: IterateeState[T]) extends State[T]
  /**
   * A Subscription completed by the Publisher.
   */
  final case object Completed extends State[Nothing]
  /**
   * A Subscription cancelled by the Subscriber.
   */
  final case object Cancelled extends State[Nothing]

  /**
   * We use an Iteratee to read from the Enumerator. Controlled by the
   * extendIteratee method.
   */
  sealed trait IterateeState[+T]
  /**
   * The Iteratee state before any elements have been requested, before
   * we've attached an Iteratee to the Enumerator.
   */
  final case object Unattached extends IterateeState[Nothing]
  /**
   * The Iteratee state when we're reading from the Enumerator.
   */
  final case class Attached[T](link: Promise[Iteratee[T, Unit]]) extends IterateeState[T]

}

import EnumeratorSubscription._

/**
 * Adapts an Enumerator to a Publisher.
 */
private[streams] class EnumeratorSubscription[T, U >: T](
  enum: Enumerator[T],
  emptyElement: Option[T],
  subr: Subscriber[U],
  onSubscriptionEnded: SubscriptionHandle[U] => Unit)
    extends StateMachine[State[T]](initialState = Requested[T](0, Unattached))
    with Subscription with SubscriptionHandle[U] {

  // SubscriptionHandle methods

  override def start(): Unit = {
    subr.onSubscribe(this)
  }

  override def subscriber: Subscriber[U] = subr

  override def isActive: Boolean = {
    // run immediately, don't need to wait for exclusive access
    state match {
      case Requested(_, _) => true
      case Completed | Cancelled => false
    }
  }

  // Streams methods

  override def request(elements: Long): Unit = {
    if (elements <= 0) throw new IllegalArgumentException(s"The number of requested elements must be > 0: requested $elements elements")
    exclusive {
      case Requested(0, its) =>
        state = Requested(elements, extendIteratee(its))
      case Requested(n, its) =>
        state = Requested(n + elements, its)
      case Completed | Cancelled =>
        () // FIXME: Check rules
    }
  }

  override def cancel(): Unit = exclusive {
    case Requested(_, its) =>
      val cancelLink: Iteratee[T, Unit] = Done(())
      its match {
        case Unattached =>
          enum(cancelLink)
        case Attached(link0) =>
          link0.success(cancelLink)
      }
      state = Cancelled
    case Cancelled | Completed =>
      ()
  }

  // Methods called by the iteratee when it receives input

  /**
   * Called when the Iteratee received Input.El, or when it recived
   * Input.Empty and the Publisher's `emptyElement` is Some(el).
   */
  private def elementEnumerated(el: T): Unit = exclusive {
    case Requested(1, its) =>
      subr.onNext(el)
      state = Requested(0, its)
    case Requested(n, its) =>
      subr.onNext(el)
      state = Requested(n - 1, extendIteratee(its))
    case Cancelled =>
      ()
    case Completed =>
      throw new IllegalStateException("Shouldn't receive another element once completed")
  }

  /**
   * Called when the Iteratee received Input.Empty and the Publisher's
   * `emptyElement` value is `None`
   */
  private def emptyEnumerated(): Unit = exclusive {
    case Requested(n, its) =>
      state = Requested(n, extendIteratee(its))
    case Cancelled =>
      ()
    case Completed =>
      throw new IllegalStateException("Shouldn't receive an empty input once completed")
  }

  /**
   * Called when the Iteratee received Input.EOF
   */
  private def eofEnumerated(): Unit = exclusive {
    case Requested(_, _) =>
      subr.onComplete()
      state = Completed
    case Cancelled =>
      ()
    case Completed =>
      throw new IllegalStateException("Shouldn't receive EOF once completed")
  }

  /**
   * Called when the Iteratee returned by the Enumerator application
   * enters a failure state. This may indicate that an error occurred
   * during evaluation of the Enumerator.
   */
  private def enumeratorApplicationFailed(): Unit = exclusive {
    case Requested(_, _) =>
      subr.onComplete()
      state = Completed
    case Cancelled =>
      ()
    case Completed =>
      () // not sure if this can happen
  }

  /**
   * Called when we want to read an input element from the Enumerator. This
   * method attaches an Iteratee to the end of the Iteratee chain. The
   * Iteratee it attaches will call one of the `*Enumerated` methods when
   * it recesives input.
   */
  private def extendIteratee(its: IterateeState[T]): IterateeState[T] = {
    val link = Promise[Iteratee[T, Unit]]()
    val linkIteratee: Iteratee[T, Unit] = Iteratee.flatten(link.future)
    val iteratee: Iteratee[T, Unit] = Cont { input =>
      input match {
        case Input.El(el) =>
          elementEnumerated(el)
        case Input.Empty =>
          emptyElement match {
            case None => emptyEnumerated()
            case Some(el) => elementEnumerated(el)
          }
        case Input.EOF =>
          eofEnumerated()
      }
      linkIteratee
    }
    its match {
      case Unattached =>
        enum(iteratee).onFailure {
          case _ => enumeratorApplicationFailed()
        }(Execution.trampoline)
      case Attached(link0) =>
        link0.success(iteratee)
    }
    Attached(link)
  }

}