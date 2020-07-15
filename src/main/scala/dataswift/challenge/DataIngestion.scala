package dataswift.challenge

import cats.effect._
import cats.implicits._
import org.http4s.Uri
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.blaze._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

case class DataIngestion(ec: ExecutionContext) {

  val baseUri = uri"https://api.github.com"

  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)
  val clientResource: Resource[IO, Client[IO]] = BlazeClientBuilder[IO](ec).resource

  def usersFromGithub(lastIdRead: Long = 0): IO[List[User]] = clientResource.use {
    _.expect[List[User]]((baseUri / "users").withQueryParam("since", lastIdRead))(jsonOf[IO,List[User]])
  }

  def userIngestionFlow(store: DataStore[IO]): IO[Unit] = {
    for {
      lastUpdate <- store.lastUpdate.getLastUpdate[User]
      newGhUsers <- usersFromGithub(lastUpdate.map(_.lastId).getOrElse(0))
      _ <- newGhUsers.map(store.users.addUser).sequence
      _ <- store.lastUpdate.addLastUpdate(LastUpdate[User](newGhUsers.last.id, System.currentTimeMillis()))
    } yield ()
  }
}

