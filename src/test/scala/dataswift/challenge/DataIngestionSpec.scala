package dataswift.challenge

import cats.effect.{ContextShift, Effect, ExitCode, IO, Timer}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpRoutes, Uri}
import org.specs2.mutable.Specification

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.global

class DataIngestionSpec extends Specification {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val ingestion = new DataIngestion(global) {
    override val baseUri: Uri = uri"http://localhost:8080"
  }

  val run = MockGHWebService.server.start.unsafeRunSync()
  "DataIngestionTest" should {
    "usersFromGithub from the beginning" in {
      ingestion.usersFromGithub().unsafeRunSync() should_== List(
        User(1, "mojombo","https://api.github.com/users/mojombo/orgs"),
        User(2, "defunkt","https://api.github.com/users/defunkt/orgs"),
        User(3, "pjhyett","https://api.github.com/users/pjhyett/orgs"),
        User(4, "wycats","https://api.github.com/users/wycats/orgs"),
      )
    }

    "usersFromGithub from the second page" in {
      ingestion.usersFromGithub(1).unsafeRunSync() should_== List(
        User(5, "ezmobius","https://api.github.com/users/ezmobius/orgs"),
        User(6, "ivey","https://api.github.com/users/ivey/orgs"),
        User(7, "evanphx","https://api.github.com/users/evanphx/orgs")
      )
    }

    "userIngestionFlow" in {

      val mockDataStore = MockDataStore()

      ingestion.userIngestionFlow(mockDataStore.ds).unsafeRunSync()

      mockDataStore.storedUsers should_== List(
        User(1, "mojombo","https://api.github.com/users/mojombo/orgs"),
        User(2, "defunkt","https://api.github.com/users/defunkt/orgs"),
        User(3, "pjhyett","https://api.github.com/users/pjhyett/orgs"),
        User(4, "wycats","https://api.github.com/users/wycats/orgs"),
      ).to[ArrayBuffer]

      mockDataStore.lastUpdateValue.lastId should_== 4
      mockDataStore.lastUpdateValue.resource should_== "User"

    }
  }

}

class MockGHWebService[F[_]: Effect] extends Http4sDsl[F] {

  object SinceQueryParamMatcher extends QueryParamDecoderMatcher[Long]("since")
  //:? TimestampQueryParamMatcher

  val service = HttpRoutes.of[F]{
    case GET -> Root / "users" :? SinceQueryParamMatcher(0)  =>
      Ok{
        """
          |[
          |  {
          |    "login": "mojombo",
          |    "id": 1,
          |    "organizations_url": "https://api.github.com/users/mojombo/orgs"
          |  },
          |  {
          |    "login": "defunkt",
          |    "id": 2,
          |    "organizations_url": "https://api.github.com/users/defunkt/orgs"
          |  },
          |  {
          |    "login": "pjhyett",
          |    "id": 3,
          |    "organizations_url": "https://api.github.com/users/pjhyett/orgs"
          |  },
          |  {
          |    "login": "wycats",
          |    "id": 4,
          |    "organizations_url": "https://api.github.com/users/wycats/orgs"
          |  }
          |]
          |""".stripMargin }
    case GET -> Root / "users" :? SinceQueryParamMatcher(1)  =>
      Ok{
        """[
          | {
          |    "login": "ezmobius",
          |    "id": 5,
          |    "organizations_url": "https://api.github.com/users/ezmobius/orgs"
          |  },
          |  {
          |    "login": "ivey",
          |    "id": 6,
          |    "organizations_url": "https://api.github.com/users/ivey/orgs"
          |  },
          |  {
          |    "login": "evanphx",
          |    "id": 7,
          |    "organizations_url": "https://api.github.com/users/evanphx/orgs"
          |  }
          |]""".stripMargin
      }
  }.orNotFound
}

object MockGHWebService {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val web = new MockGHWebService[IO]

  val server = BlazeServerBuilder[IO](global)
    .bindHttp(8080)
    .withHttpApp(web.service)
    .serve
    .compile
    .drain
    .as(ExitCode.Success)
}
