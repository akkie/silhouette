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
package silhouette.util

/**
 * Represents a reader that transform an instance of `A` to `B`.
 *
 * @tparam A The source type.
 * @tparam B The target type.
 */
trait Reads[A, B] { self =>

  /**
   * Transforms from source `A` to target `B`.
   *
   * @param in The source to transform.
   * @return An instance of `B`.
   */
  def read(in: A): B

  /**
   * Composes this [[Reads]] with a transformation function that gets applied to the result of this [[Reads]].
   *
   * @param reads The transformation function.
   * @tparam C The result type of the transformation function.
   * @return A new composable [[Reads]].
   */
  def andThen[C](reads: Reads[B, C]): Reads[A, C] = (in: A) => reads.read(self.read(in))
}

/**
 * Represents a writer that transform an instance of `A` to `B`.
 *
 * @tparam A The source type.
 * @tparam B The target type.
 */
trait Writes[A, B] { self =>

  /**
   * Transforms from source `A` to target `B`.
   *
   * @param in The source to transform.
   * @return An instance of `B`.
   */
  def write(in: A): B

  /**
   * Composes this [[Writes]] with a transformation function that gets applied to the result of this [[Writes]].
   *
   * @param writes The transformation function.
   * @tparam C The result type of the transformation function.
   * @return A new composable [[Writes]].
   */
  def andThen[C](writes: Writes[B, C]): Writes[A, C] = (in: A) => writes.write(self.write(in))
}
