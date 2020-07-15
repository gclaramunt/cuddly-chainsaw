package dataswift.challenge

import cats.effect.Effect
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._


class ChallengeWebService[F[_]: Effect](ds: DataStore[F]) extends Http4sDsl[F] {

  val service = HttpRoutes.of[F]{
      case GET -> Root / "challenge"  =>
        Ok{ ds.users.getAllUsers.map(_.asJson) }
    }.orNotFound
}
