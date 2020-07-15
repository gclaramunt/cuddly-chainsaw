package dataswift.challenge

import cats.effect.IO
import org.http4s.{HttpRoutes, Method, Request, Response, Uri}
import org.specs2.mutable.Specification
import org.http4s.implicits._
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

import scala.collection.mutable.ArrayBuffer


class ChallengeWebServiceSpec extends Specification {

  "ChallengeWebService" should {
    "Get stored GH users list" in {

      val data = List(
        User(1, "mojombo","https://api.github.com/users/mojombo/orgs"),
        User(2, "defunkt","https://api.github.com/users/defunkt/orgs"),
        User(3, "pjhyett","https://api.github.com/users/pjhyett/orgs"),
        User(4, "wycats","https://api.github.com/users/wycats/orgs"),
      )

      MockDataStore.storedUsers.clear()
      MockDataStore.storedUsers ++= data.to[ArrayBuffer]

      val get = Request[IO](Method.GET, uri"/challenge")
      val response = new ChallengeWebService(MockDataStore.ds).service.run(get).unsafeRunSync()

      response.as[Json].unsafeRunSync() should_== data.asJson
    }
  }
}
