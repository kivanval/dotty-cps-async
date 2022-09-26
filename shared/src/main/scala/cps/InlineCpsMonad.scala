package cps

import scala.util.Try

/**
 * Inline cps monad which can used for
 * (usefull for deep embedding of other languages into scala, sending data over network, etc..)   
 **/
trait InlineCpsMonad[F[_]] extends CpsContextCarrier[F] {

    inline def lazyPure[A](inline a:A): F[A]

    inline def pure[A](inline a:A): F[A]

    inline def map[A,B](inline fa:F[A])(f: A=>B):F[B]

    inline def flatMap[A,B](inline fa: F[A])(f: A=>F[B]):F[B]

}


trait InlineCpsTryMonad[F[_]] extends InlineCpsMonad[F] {

    inline def error[A](inline e: Exception): F[A]

    inline def mapTry[A,B](inline fa:F[A])(inline f: Try[A]=>B):F[B] =
        flatMapTry(fa)(x => pure(f(x)))

    inline def flatMapTry[A,B](inline fa: F[A])(inline f: Try[A]=>F[B]):F[B]

}

