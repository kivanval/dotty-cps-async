package cps.compat

import scala.concurrent._
import cps._
import cps.monads.{ given CpsAsyncMonad[Future] }

/**
 * Import this object instead cps.*  to receive SIP22-compabiliy interface.
 **/
object FutureAsync:

  inline def async[T](inline x:T)(using ec:ExecutionContext):Future[T]=
  ${  
    cps.macros.Async.transformContextInstanceMonad('x, '{FutureAsyncMonad(using ec)} )
  }

  inline def await[T](x:Future[T])(using ec: ExecutionContext):T = 
                       cps.await[Future, T](x)(using FutureAsyncMonad(using ec))


val sip22 = FutureAsync


