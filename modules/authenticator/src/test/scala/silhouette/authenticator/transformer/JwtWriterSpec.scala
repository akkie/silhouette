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
package silhouette.authenticator.transformer

import java.time.Instant

import cats.effect.IO
import io.circe.syntax._
import io.circe.{ Json, JsonObject }
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import silhouette.LoginInfo
import silhouette.authenticator.Authenticator
import silhouette.crypto.Base64
import silhouette.jwt.{ Claims, JwtClaimWriter }

/**
 * Test case for the [[JwtWriter]] class.
 */
class JwtWriterSpec extends Specification with Mockito {

  "The `write` method" should {
    "write a claims representation from the authenticator" in new Context {
      claimWriter(any[Claims]()) returns Right(jwt)

      val captor = capture[Claims]

      jwtWriter(authenticator).unsafeRunSync() must be equalTo jwt
      there was one(claimWriter).apply(captor)

      captor.value must be equalTo claims
    }
  }

  /**
   * The context.
   */
  trait Context extends Scope {

    /**
     * A test JWT.
     *
     * This isn't a valid JWT, but a valid JWT is not needed because we mock the JWT implementation.
     */
    val jwt = "a.test.jwt"

    /**
     * An instant of time.
     */
    val instant = Instant.parse("2017-10-22T20:50:45.0Z")

    /**
     * The login info.
     */
    val loginInfo = LoginInfo("credentials", "john@doe.com")

    /**
     * A JWT claims object.
     */
    val claims = Claims(
      issuer = Some("issuer"),
      subject = Some(Base64.encode(loginInfo.asJson.toString())),
      audience = Some(List("test")),
      expirationTime = Some(instant),
      notBefore = Some(instant),
      issuedAt = Some(instant),
      jwtID = Some("id"),
      custom = JsonObject(
        "tags" -> Json.arr(Json.fromString("tag1"), Json.fromString("tag2")),
        "fingerprint" -> Json.fromString("fingerprint"),
        "payload" -> Json.obj("secure" -> Json.True)
      )
    )

    /**
     * The authenticator representation of the claims object.
     */
    val authenticator = Authenticator(
      id = "id",
      loginInfo = loginInfo,
      touched = Some(instant),
      expires = Some(instant),
      fingerprint = Some("fingerprint"),
      tags = Seq("tag1", "tag2"),
      payload = Some(Json.obj("secure" -> Json.True))
    )

    /**
     * A mock of the underlying JWT claim writer.
     */
    val claimWriter = mock[JwtClaimWriter]

    /**
     * The JWT authenticator writer.
     */
    val jwtWriter = JwtWriter[IO](claimWriter, claims.issuer, claims.audience, claims.notBefore)
  }
}
