package dataswift.challenge

import cats.effect.{ExitCode, IO, IOApp}
import doobie.util.transactor.Transactor
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

object ChallengeServer extends IOApp{

  override def run(args: List[String]): IO[ExitCode] = {

    val xa = Transactor.fromDriverManager[IO](
      "org.h2.Driver",
      "jdbc:h2:./data-store.db;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/main/resources/createdb.sql'",
      "sa", // user
      "" // password
    )

    val dataStore = DataStore(xa)

    import cats.implicits._

    def repeatIngest: IO[Unit] = DataIngestion(global).userIngestionFlow(dataStore) >> IO.sleep(10.seconds) >> IO.suspend(repeatIngest)

    val challengeWS = new ChallengeWebService(dataStore)

    val server = BlazeServerBuilder[IO](global)
      .bindHttp(8080)
      .withHttpApp(challengeWS.service)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

    for {
      _ <- repeatIngest.start
      result <- server
    } yield {
      result
    }
  }
}