package dataswift.challenge

import cats.effect.{Effect, IO}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

object  MockDataStore {

  val storedUsers = new ArrayBuffer[User]()
  var lastUpdateValue: LastUpdate = _

  class MockUserStore[F[_]:Effect] extends UserStore(null) {

    override lazy val getAllUsers: fs2.Stream[F, User] = fs2.Stream.emits(storedUsers)

    override def addUser(user: User): F[Int] = {
      storedUsers += user
      implicitly[Effect[F]].pure(1)
    }
  }

  class MockLastUpdateStore[F[_]:Effect] extends LastUpdateStore(null) {

    override def getLastUpdate[T: ClassTag]: F[Option[LastUpdate]] ={
      implicitly[Effect[F]].pure(None)
    }

    override def addLastUpdate(update: LastUpdate): F[Int] = {
      lastUpdateValue = update
      implicitly[Effect[F]].pure(1)
    }
  }


  val ds = new DataStore[IO](null) {
    override lazy val users = new MockUserStore
    override lazy val lastUpdate = new MockLastUpdateStore
  }
}
