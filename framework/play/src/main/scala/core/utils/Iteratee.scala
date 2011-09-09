package play.core

object Iteratee {
 
    trait Input[+E]{
        def map[U](f:(E => U) ):Input[U] = this match {
            case El(e) => El(f(e))
            case Empty => Empty
            case EOF => EOF
        }
    }
    case class El[E](e: E) extends Input[E]
    case object Empty extends Input[Nothing]
    case object EOF extends Input[Nothing]


    def flatten[E,A](i:Promise[Iteratee[E,A]]):Iteratee[E,A] = new Iteratee[E,A] {

        def fold[B](done: (A,Input[E]) => Promise[B],
                    cont: (Input[E] => Iteratee[E,A]) => Promise[B],
                    error: (String,Input[E]) => Promise[B]):Promise[B] = i.flatMap(_.fold(done,cont,error))
    }

trait Iteratee[E,+A] {
    self =>
    def run[AA >: A]:Promise[AA] = fold ( (a,_) => Promise.pure(a),
                                          k => k(EOF).fold((a1,_) => Promise.pure(a1),
                                                            _ => error("diverging iteratee after EOF"),
                                                            (msg,e) => error(msg)),
                                          (msg,e) => error(msg))
    
    def fold[B](done: (A,Input[E]) => Promise[B],
                cont: (Input[E] => Iteratee[E,A]) => Promise[B],
                error: (String,Input[E]) => Promise[B]):Promise[B]


    def flatMap[B](f : A => Iteratee[E,B] ):Iteratee[E,B] = new Iteratee[E,B] {
        
        def fold[C](done: (B,Input[E]) => Promise[C],
                    cont: (Input[E] => Iteratee[E,B]) => Promise[C],
                    error: (String,Input[E]) => Promise[C]) = 

             self.fold( {case (a,Empty) => f(a).fold(done,cont,error)
                         case (a,e) => f(a).fold( (a,_) => done(a,e),
                                                           k => cont(k),
                                                           error)},
                       ((k) => cont(e => (k(e).flatMap(f)))),
                       error)
   
    }




}

object Done {
    def apply[E,A](a: A, e:Input[E]) : Iteratee[E,A] = new Iteratee[E,A]{
        def fold[B](done: (A,Input[E]) => Promise[B],
                    cont: (Input[E] => Iteratee[E,A]) => Promise[B],
                    error: (String,Input[E]) => Promise[B]):Promise[B] = done(a,e)

    }

}

object Cont{
    def apply[E,A](k: Input[E] => Iteratee[E,A]): Iteratee[E,A]= new Iteratee[E,A]{
        def fold[B](done: (A,Input[E]) => Promise[B],
                    cont: (Input[E] => Iteratee[E,A]) => Promise[B],
                    error: (String,Input[E]) => Promise[B]):Promise[B] = cont(k)

    }
}
object Error{
    def apply[E](msg:String,e:Input[E]): Iteratee[E,Nothing] = new  Iteratee[E,Nothing] {
        def fold[B](done: (Nothing,Input[E]) => Promise[B],
                    cont: (Input[E] => Iteratee[E,Nothing]) => Promise[B],
                    error: (String,Input[E]) => Promise[B]):Promise[B] = error(msg,e)

    }
}

trait Enumerator[+E]{

    parent =>
    def apply[A,EE >: E](i:Iteratee[EE,A]):Promise[Iteratee[EE,A]]
    def <<:[A,EE >: E](i:Iteratee[EE,A]):Promise[Iteratee[EE,A]] = apply(i)



    def map[U](f: Input[E] => Input[U]) = new Enumerator[U] {
        def apply[A,UU >: U](it:Iteratee[UU,A]) = {

            case object OuterEOF extends Input[Nothing]
            type R = Iteratee[E,Iteratee[UU,A]]

            def step(ri:Iteratee[UU,A])(in:Input[E]) : R  =

                in match {
                    case OuterEOF => Done(ri,EOF)
                    case any => 
                        flatten(
                            ri.fold( (a,_) => Promise.pure(Done(ri,any)),
                                      k => {val next = k(f(any))
                                            next.fold( (a,_) => Promise.pure(Done(next,in)),
                                                        _ => Promise.pure(Cont(step(next))),
                                                        (msg,_) => Promise.pure[R](Error(msg,in) ))},
                                        (msg,_) => Promise.pure[R](Error(msg,any))))
                }
            
            parent.apply(Cont(step(it)))
                  .flatMap( _.fold( (a,_) => Promise.pure(a),
                                   k => k(OuterEOF).fold(
                                       (a1,_) => Promise.pure(a1),
                                       _ => error("diverging iteratee after EOF"),
                                       (msg,e) => error(msg)),
                                       (msg,e) => error(msg)))
        }
    }

}

object Enumerator {

    def enumInput[E](e:Input[E]) = new Enumerator[E]{
        def apply[A,EE >: E](i:Iteratee[EE,A]):Promise[Iteratee[EE,A]] =
            i.fold((a,e) => Promise.pure(i),
                   k => Promise.pure(k(e)),
                   (_,_) => Promise.pure(i) )

    }

    def empty[A] = enumInput[A](EOF)

    def apply[E](in:E *):Enumerator[E] = new Enumerator[E]{

        def apply[A,EE >: E](i:Iteratee[EE,A]) : Promise[Iteratee[EE,A]] = enumerate(in,i)

    }
    def enumerate[E,A]: ( Seq[E], Iteratee[E,A] ) => Promise[Iteratee[E,A]] = {
        (l,i) =>  l.foldLeft(Promise.pure(i)) ((i,e) => 
                    i.flatMap(_.fold((_,_) => i,
                                     k =>Promise.pure(k(El(e))),
                                     (_,_) => i ) ) )
    }
}
trait PromiseValue[+A]

trait NotWaiting[+A] extends PromiseValue[A]
case class Thrown(e:Exception) extends NotWaiting[Nothing]
case class Redeemed[+A](a:A) extends NotWaiting[A]
case object Waiting extends PromiseValue[Nothing]

trait Promise[A]{

  def onRedeem(k: A => Unit):Unit 

  def extend[B](k:Function1[Promise[A],B]):Promise[B]

  def value: PromiseValue[A]

  def filter(p: A => Boolean) : Promise[A]

  def map[B](f: A => B): Promise[B]

  def flatMap[B](f: A => Promise[B]) : Promise[B]
}

trait Redeemable[A]{ 
    def redeem(a: => A):Unit
}

trait RIPPromise[A] extends Promise[A] {
    def value: NotWaiting[A]
}

class STMPromise[A] extends Promise[A] with Redeemable[A] {
  import scala.concurrent.stm._

  val actions :Ref[List[Promise[A] => Unit]] = Ref(List())
  var redeemed:Ref[Option[A]] =Ref(None)

  def extend[B](k:Function1[Promise[A],B]):Promise[B] = {
      val result = new STMPromise[B]()
      onRedeem(_ => result.redeem(k(this)))
      result
  }

  def filter(p: A => Boolean):Promise[A] = {
      val result = new STMPromise[A]()
      onRedeem(a => if(p(a)) result.redeem(a))
      result
  }

  def collect[B](p: PartialFunction[A,B]) = {
      val result = new STMPromise[B]()
      onRedeem(a => p.lift(a).foreach( result.redeem(_)))
      result
  }

  def onRedeem(k: A => Unit) : Unit = {
      addAction( p => p.value match { case Redeemed(a) => k(a); case _ => })

  }

  private def addAction(k: Promise[A] => Unit):Unit = {
      if(redeemed.single().isDefined){
        redeemed.single().foreach(_ => k(this))
      } else {
         val ok:Boolean =  atomic { implicit txn =>
              if(!redeemed().isDefined){ actions() = actions() :+ k ; true}
              else false
              }
          if(!ok) redeemed.single().foreach(_ => invoke(this,k))
      }
  }

  def value : PromiseValue[A] = redeemed.single().map(Redeemed(_)).getOrElse(Waiting)

  private def invoke[T](a:T, k:T=> Unit) = PromiseInvoker.invoker ! Invoke(a,k)

  def redeem(body: => A):Unit = {
      val result = scala.util.control.Exception.allCatch[A].either(body)
      atomic { implicit txn => 
          if(redeemed().isDefined) error("already redeemed")
          result.right.foreach{ a =>
              redeemed() = Some(a)
          }
     }
     actions.single.swap(List()).foreach(invoke(this,_)) //need to remove them from the list
  }

  def map[B](f: A => B): Promise[B] = {
      val result = new STMPromise[B]()
      this.addAction(p => p.value match {
          case Redeemed(a) => result.redeem(f(a))
          case Thrown(e) => result.redeem(throw e)
      })
      result
  }

  def flatMap[B](f: A => Promise[B]) = {
      val result = new STMPromise[B]()
      this.addAction(p => p.value match {
          case Redeemed(a) => 
              f(a).extend(ip => ip.value match {
                  case Redeemed(a) => result.redeem(a)
                  case Thrown(e) => result.redeem(throw e)

              })
          case Thrown(e) => result.redeem(throw e)
      })
      result
  }
}

object PurePromise{

    def apply[A](a:A): Promise[A] = new Promise[A] {

        private def neverRedeemed[A] :Promise[A] = new Promise[A] {
              def onRedeem(k: A => Unit):Unit = ()

              def extend[B](k:Function1[Promise[A],B]):Promise[B] = neverRedeemed[B]

              def value: PromiseValue[A] = Waiting

              def filter(p: A => Boolean) : Promise[A] = this

              def map[B](f: A => B): Promise[B] = neverRedeemed[B]

              def flatMap[B](f: A => Promise[B]) : Promise[B] = neverRedeemed[B]

        }

        def onRedeem(k: A => Unit) :Unit = k(a)

        def value = Redeemed(a)

        def redeem(a:A) = error("Already redeemed")

        def extend[B](f:(Promise[A] => B)):Promise[B] = {
           apply(f(this))
        }

        def filter(p:A => Boolean) = if (p(a)) this else neverRedeemed[A]

        def map[B](f: A => B): Promise[B] = PurePromise[B](f(a))

        def flatMap[B](f: A => Promise[B]) : Promise[B] = f(a)
    }
}

object Promise{
    def pure[A](a:A) = PurePromise(a)
    def apply[A]() = new STMPromise[A]()
}

def fold[E,A](state:A)(f :(A,E) => A):Iteratee[E,A]=
{
    def step(s:A)(i:Input[E]):Iteratee[E,A]= i match{

        case EOF => Done(s, EOF)
        case Empty => Cont[E,A]( i => step(s)(i))
        case El(e) => { val s1=f(s,e);  Cont[E,A]( i => step(s1)(i)) }
    }
    (Cont[E,A](i => step(state)(i)))
}

def mapChunk_[E](f: E => Unit) :Iteratee[E,Unit] = fold[E,Unit](())((_,e) => f(e) )
}
