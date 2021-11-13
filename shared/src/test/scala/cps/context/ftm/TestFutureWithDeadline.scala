package cps.context.ftm

import scala.concurrent.*
import scala.concurrent.duration.*
import scala.util.*
import scala.util.control.NonFatal

import cps.*
import cps.monads.{*,given}

import java.util.Timer
import java.util.TimerTask

import org.junit.{Test,Ignore}
import org.junit.Assert.*
import cps.util.*

import scala.concurrent.ExecutionContext.Implicits.global 


/**
 * Future with deadline which can be reset.
 **/
enum FutureWithDeadline[T]:

    case Computation[T](promise: Promise[T], deadlineContext: DeadlineContext) extends FutureWithDeadline[T]
    case Value(result: Try[T]) extends FutureWithDeadline[T]

    def future: Future[T] =
        this match
            case Computation(p,c) => p.future
            case Value
    (r) => Future.fromTry(r)

    def value: Option[Try[T]] = future.value

//TODO: place inside enum, fill bug in not resolved

given cpsMonad(using ExecutionContext): FutureWithDeadlineCpsMonad = 
    FutureWithDeadlineCpsMonad()

given cpsMonadConversion(using DeadlineContext): CpsMonadConversion[Future,FutureWithDeadline] with {

    def apply[T](ft:Future[T]): FutureWithDeadline[T] =
        summon[DeadlineContext].addComputation(ft)
        
}


class DeadlineContext(initDeadline: Long) {

    private[this] var deadline: Long = initDeadline;
    private[this] val timeoutPromise: Promise[Nothing] = Promise()
    private[this] var lastTimerTask: TimerTask | Null = null

    def timeoutFuture: Future[Nothing] = timeoutPromise.future

    def setTimeout(newTimeout: Duration): Unit =
        setDeadline( now() + newTimeout.toMillis );

    def setDeadline(newDeadline: Long) =
        this.synchronized{
          if !(lastTimerTask eq null) then 
               lastTimerTask.cancel()
          deadline = newDeadline;
          if (deadline > 0L) then
             val delay = now() - deadline
             if (delay < 0) then
                throw new IllegalStateException("attempt to set deadlin in past")
             else
                lastTimerTask = new TimerTask() {
                    override def run(): Unit = {
                        timeoutPromise.tryFailure(new TimeoutException())
                    }
                }
                DeadlineContext.timer.schedule(lastTimerTask, delay)  
           else  
             lastTimerTask = null
        }

    def isExpired(): Boolean =
        deadline > 0 && deadline <= now()

    def now(): Long =
        // in real life shoulc be abstract, 
        System.currentTimeMillis()

    def addComputation[T]( x: => Future[T]):FutureWithDeadline[T] =
        val p = Promise[T]
        p.tryCompleteWith(timeoutFuture)
        p.tryCompleteWith(x)
        FutureWithDeadline.Computation(p, this)

}

object DeadlineContext {

    // timer to be compatible with scala-js
    val timer = new Timer()

}

class FutureWithDeadlineCpsMonad(using ExecutionContext) extends CpsMonad[FutureWithDeadline] with CpsAsyncMonad[FutureWithDeadline] {

    override type Context = DeadlineContext

    def pure[T](t:T):FutureWithDeadline[T] = FutureWithDeadline.Value(Success(t))

    def map[A,B](fa:FutureWithDeadline[A])(f: A=>B):FutureWithDeadline[B] =
        fa match
            case FutureWithDeadline.Computation(pa,ca) =>
                ca.addComputation(pa.future.map(f))
            case FutureWithDeadline.Value(r) =>
                r match
                    case Success(x) => 
                        tryValue(f(x))
                    case Failure(ex) =>
                        failure(ex)
       
    def flatMap[A,B](fa:FutureWithDeadline[A])(f: A=> FutureWithDeadline[B]): FutureWithDeadline[B] =
        fa match
            case FutureWithDeadline.Computation(pa,ca) =>
                ca.addComputation(fa.future.flatMap{ a =>
                    try 
                        f(a).future
                    catch
                        case NonFatal(ex) =>
                            Future failed ex
                })
            case FutureWithDeadline.Value(r) =>
                r match
                    case Success(a) =>
                       tryComputation(f(a))
                    case Failure(ex) =>
                        failure(ex)


    def apply[T](op: Context => FutureWithDeadline[T]): FutureWithDeadline[T] =
        val context  = new DeadlineContext(-1)
        op(context)
 
    def adoptAwait[A](c:Context, fa:FutureWithDeadline[A]):FutureWithDeadline[A] =
        c.addComputation(fa.future)    
        
    def error[A](e: Throwable): FutureWithDeadline[A] =
        FutureWithDeadline.Value(Failure(e))

    def flatMapTry[A,B](fa:FutureWithDeadline[A])(f: Try[A] => FutureWithDeadline[B]):FutureWithDeadline[B] =
        fa match
            case FutureWithDeadline.Value(ra) => 
                tryComputation(f(ra))
            case FutureWithDeadline.Computation(pa,ca) =>
                ca.addComputation(
                    pa.future.transformWith{ ra =>
                        f(ra).future
                    }
                )
                        

    def adoptCallbackStyle[A](source: (Try[A]=>Unit) => Unit): FutureWithDeadline[A] =
        val p = Promise[A]
        source(p.tryComplete(_))
        apply(ctx => FutureWithDeadline.Computation(p,ctx))

    def tryValue[A](f: =>A): FutureWithDeadline[A] =
        try
            FutureWithDeadline.Value(Success(f))
        catch
            case NonFatal(ex) => failure(ex)

    def tryComputation[A](f: => FutureWithDeadline[A]): FutureWithDeadline[A] =
        try
            f
        catch
            case NonFatal(ex) => failure(ex)
            

    def failure[A](ex: Throwable): FutureWithDeadline[A] =
        FutureWithDeadline.Value(Failure(ex))


}

class TestFutureWithDeadline:

  
  @Test def testTimeoutedAwait() = 
      var x = 0
      val c = async[FutureWithDeadline] {
        summon[DeadlineContext].setTimeout(100.millis)
        await(FutureSleep(1000.millis))
        x = 1
      }
      c.future.transform{ 
         case Success(x) => Failure(new IllegalStateException("Timeout exception was expected"))
         case Failure(ex) =>
             assert (x == 0)
             assert(ex.isInstanceOf[TimeoutException])
             Success(())
      }
  