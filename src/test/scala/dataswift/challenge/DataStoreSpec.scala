package dataswift.challenge
import cats.effect._
import doobie._
import doobie.implicits._
import org.specs2.mutable.Specification

class DataStoreSpec extends Specification {

  sequential

  // We need a ContextShift[IO] before we can construct a Transactor[IO]. The passed ExecutionContext
  // is where nonblocking operations will be executed. For testing here we're using a synchronous EC.
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
  // on an our synchronous EC. See the chapter on connection handling for more info.
  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:./data-store-test.db;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/main/resources/createdb.sql'",
    "sa", // user
    "", // password
    Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
  )

  val ds = DataStore(xa)

  def clearDb() =  for {
    - <- sql"""TRUNCATE TABLE USER""".update.run.transact(xa)
    _ <- sql"""TRUNCATE TABLE LAST_UPDATE""".update.run.transact(xa)
  } yield ()

  "UserStoreTest" should {
    "addUser" in {
      val user = User(666,"Demian Omen", "http://hell.org")

      (for {
        _ <- clearDb()
        _ <- ds.users.addUser(user)
        result <- sql"SELECT id, login, organizations_url FROM user WHERE id = ${user.id}"
          .query[User]
          .unique
          .transact(xa)
      } yield { result should_== user }).unsafeRunSync()

    }

    "getAllUsers" in {

      (for {
        _ <- clearDb()
        _ <-
          sql"""
            INSERT INTO USER (id, login, organizations_url ) VALUES (123, 'test-user', 'http://url.com')
            """.update.run.transact(xa)
        _ <-
          sql"""
            INSERT INTO USER (id, login, organizations_url ) VALUES (124, 'test-user1', 'http://url2.com')
            """.update.run.transact(xa)
        result <- ds.users.getAllUsers.compile.toList
      } yield {
        result should_== List(
          User(123, "test-user", "http://url.com"),
          User(124, "test-user1", "http://url2.com")
        )
      }).unsafeRunSync()
    }
  }

  "LastUpdateStoreTest" should {
    "addLastUpdate" in {
      val lu = LastUpdate[User](1234, System.currentTimeMillis())

      (for {
        _ <- clearDb()
        _ <- ds.lastUpdate.addLastUpdate(lu)
        result <- sql"SELECT resource, last_id, timestamp FROM LAST_UPDATE WHERE last_id = ${lu.lastId}"
          .query[LastUpdate]
          .unique
          .transact(xa)
      } yield { result should_== lu }).unsafeRunSync()
    }

    "getLastUpdate" in {
      val lu = LastUpdate[User](12, 11111111)

      ( for {
        _ <- clearDb()
        _ <-
          sql"""
                  INSERT INTO LAST_UPDATE (resource, last_id, timestamp ) VALUES (${lu.resource}, 12, 11111111)
                  """.update.run.transact(xa)
        data <- ds.lastUpdate.getLastUpdate[User]
      } yield {
        data should_== Some(lu)
      }).unsafeRunSync()
    }
  }


}
