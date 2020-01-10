/**
 * Licensed to the Minutemen Group under one or more contributor license
 * agreements. See the COPYRIGHT file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package silhouette.authenticator.pipeline

import cats.data.{ EitherT, Kleisli }
import cats.effect.Sync

import scala.language.implicitConversions
import scala.util.Try

/**
 * A simple DSL to model authenticator pipelines.
 */
object Dsl extends DslLowPriorityImplicits {

  /**
   * Maybe is an IO monad that represents a success and an error case. It's represented by the monad transformer
   * [[EitherT]] from Cats, because it allows us to easily compose [[Either]] and `F[_]` together without writing
   * a lot of boilerplate and it makes it easy to compose it with [[Kleisli]].
   */
  type Maybe[F[_], A] = EitherT[F, Throwable, A]

  /**
   * A transformation function that transform a type `A` to [[Maybe]].
   */
  type MaybeWriter[F[_], A, B] = A => Maybe[F, B]

  /**
   * Kleisli that works with [[Maybe]].
   *
   * Represents a function `A => Maybe[F, B]`.
   */
  type KleisliM[F[_], A, B] = Kleisli[({ type L[T] = Maybe[F, T] })#L, A, B]

  /**
   * The companion object for [[Maybe]].
   */
  object Maybe {

    /**
     * Option handles a missing case with [[None]] and not really an error case. This [[Throwable]] represents the
     * [[None]] case for the [[Maybe]] type.
     */
    case object NonError extends Throwable()
  }

  /**
   * The companion object for [[KleisliM]].
   */
  object KleisliM {

    /**
     * Constructs a [[KleisliM]] from a function `A` => `B`.
     *
     * @param f      The function to convert.
     * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
     * @tparam F The type of the IO monad.
     * @tparam A The type of the function parameter.
     * @tparam B The type the function returns.
     * @tparam C The type that will be stored in [[Maybe]].
     * @return A [[KleisliM]] from a function `A` => `B`.
     */
    def apply[F[_], A, B, C](f: A => B)(
      implicit
      writes: MaybeWriter[F, B, C]
    ): KleisliM[F, A, C] = Kleisli((a: A) => writes(f(a)))
  }

  /**
   * Monkey patches a [[KleisliM]] instance.
   *
   * @param kleisliM The instance to patch.
   * @tparam F The type of the IO monad.
   * @tparam A The type of the function parameter.
   * @tparam B The type the function returns.
   */
  implicit class KleisliMOps[F[_]: Sync, A, B](kleisliM: KleisliM[F, A, B]) {

    /**
     * An alias for the [[Kleisli.andThen]] function that allows to pass a [[KleisliM]] instance.
     */
    def >>[C](k: KleisliM[F, B, C]): KleisliM[F, A, C] = kleisliM.andThen(k)
  }

  /**
   * A transformation function that transforms an [[Option]] to [[Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Maybe]] representation for the [[Option]] type.
   */
  implicit def optionToMaybeWriter[F[_]: Sync, A]: MaybeWriter[F, Option[A], A] = (value: Option[A]) =>
    EitherT.fromEither[F](value.toRight(Maybe.NonError))

  /**
   * A transformation function that transforms a [[scala.util.Try]] to [[Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Maybe]] representation for the [[scala.util.Try]] type.
   */
  implicit def tryToMaybeWriter[F[_]: Sync, A]: MaybeWriter[F, Try[A], A] = (value: Try[A]) =>
    EitherT.fromEither[F](value.toEither)

  /**
   * A transformation function that transforms an `Either[Throwable, A]` to [[Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Maybe]] representation for the `Either[Throwable, A]` type.
   */
  implicit def eitherToMaybeWriter[F[_]: Sync, A]: MaybeWriter[F, Either[Throwable, A], A] =
    (value: Either[Throwable, A]) => EitherT.fromEither[F](value)

  /**
   * A transformation function that transforms a functional effect to [[Dsl.Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Dsl.Maybe]] representation for the `Either[Throwable, A]` type.
   */
  implicit def effectToMaybeWriter[F[_]: Sync, A, B](
    implicit
    writer: Dsl.MaybeWriter[F, A, B]
  ): Dsl.MaybeWriter[F, F[A], B] = (value: F[A]) =>
    for {
      r <- EitherT.right(value)
      v <- writer(r)
    } yield v

  /**
   * Constructs a [[KleisliM]] from a function `A` => `B`.
   *
   * @param f      The function to convert.
   * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
   * @tparam F The type of the IO monad.
   * @tparam A The type of the function parameter.
   * @tparam B The type the function returns.
   * @tparam C The type that will be stored in [[Maybe]].
   * @return A [[KleisliM]] from a function `A` => `B`.
   */
  implicit def toKleisliM[F[_], A, B, C](f: A => B)(
    implicit
    writes: Dsl.MaybeWriter[F, B, C]
  ): Dsl.KleisliM[F, A, C] = Dsl.KleisliM(f)

  /**
   * Constructs a [[KleisliM]] from a function `A` => `B`.
   *
   * @param f      The function to convert.
   * @param writes The writes that transforms the value of the function `A` => `B` into a [[KleisliM]].
   * @tparam F The type of the IO monad.
   * @tparam A The type of the function parameter.
   * @tparam B The type the function returns.
   * @tparam C The type that will be stored in [[Maybe]].
   * @return A [[KleisliM]] from a function `A` => `B`.
   */
  def |>[F[_], A, B, C](f: A => B)(
    implicit
    writes: MaybeWriter[F, B, C]
  ): KleisliM[F, A, C] = KleisliM(f)
}

/**
 * Low-priority implicits for the Dsl.
 */
trait DslLowPriorityImplicits {

  /**
   * A low priority transformation function that transforms any type to [[Dsl.Maybe]].
   *
   * @tparam F The IO monad.
   * @tparam A The type to convert.
   * @return The [[Dsl.Maybe]] representation for type `A`.
   */
  implicit def toMaybeWrites[F[_]: Sync, A]: Dsl.MaybeWriter[F, A, A] = (value: A) =>
    EitherT.pure[F, Throwable](value)
}
