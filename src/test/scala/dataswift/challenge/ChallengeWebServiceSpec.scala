package dataswift.challenge

import cats.effect.IO
import io.circe._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.{Method, Request}
import org.specs2.mutable.Specification

import scala.collection.mutable.ArrayBuffer


class ChallengeWebServiceSpec extends Specification {

  "ChallengeWebService" should {
    "Get stored GH users list" in {

      val mockDataStore = MockDataStore()

      val data = List(
        User(1, "mojombo","https://api.github.com/users/mojombo/orgs"),
        User(2, "defunkt","https://api.github.com/users/defunkt/orgs"),
        User(3, "pjhyett","https://api.github.com/users/pjhyett/orgs"),
        User(4, "wycats","https://api.github.com/users/wycats/orgs"),
      )

      mockDataStore.storedUsers ++= data.to[ArrayBuffer]

      val get = Request[IO](Method.GET, uri"/challenge")
      val response = new ChallengeWebService(mockDataStore.ds).service.run(get).unsafeRunSync()

      response.as[Json].unsafeRunSync() should_== data.asJson
    }
  }
}
