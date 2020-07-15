package dataswift.challenge

import cats.effect.Effect
import doobie._
import doobie.implicits._

import scala.reflect.ClassTag


case class DataStore[F[_]:Effect](xa: Transactor[F]){

  lazy val users = new UserStore(xa)
  lazy val lastUpdate = new LastUpdateStore(xa)

}

class UserStore[F[_]:Effect](xa: Transactor[F]) {

  lazy val getAllUsers: fs2.Stream[F, User] = {
    sql"SELECT id, login, organizations_url FROM user"
      .query[User]
      .stream
      .transact(xa)
  }

  def addUser(user: User): F[Int] = {
    sql"INSERT INTO USER (id, login, organizations_url ) VALUES (${user.id}, ${user.login}, ${user.organizationsUrl})"
      .update.run.transact(xa)
  }
}

class LastUpdateStore[F[_]:Effect](xa: Transactor[F]) {

  def getLastUpdate[T: ClassTag]: F[Option[LastUpdate]] = {
    val resource = LastUpdate.resourceName[T]
    sql"SELECT resource, last_id, timestamp FROM LAST_UPDATE WHERE resource = $resource ORDER BY last_id DESC LIMIT 1"
      .query[LastUpdate]
      .option
      .transact(xa)
  }

  def addLastUpdate(update: LastUpdate): F[Int] = {
    sql"INSERT INTO LAST_UPDATE (resource, last_id, timestamp ) VALUES (${update.resource}, ${update.lastId}, ${update.timestamp})"
      .update.run.transact(xa)
  }


}

