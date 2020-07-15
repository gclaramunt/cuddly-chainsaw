package dataswift.challenge

import io.circe.{Decoder, Encoder}

import scala.reflect.ClassTag

case class User(id: Long, login: String, organizationsUrl: String)

object User {
  implicit val encodeUser: Encoder[User] =
    Encoder.forProduct3("id", "login", "organizations_url")(u =>
      (u.id, u.login, u.organizationsUrl)
    )

  implicit val decodeUser: Decoder[User] =
    Decoder.forProduct3("id", "login", "organizations_url")(User.apply)
}

case class LastUpdate( resource: String, lastId: Long, timestamp: Long)

object LastUpdate {

  def resourceName[T: ClassTag]: String = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  def apply[T: ClassTag](lastId: Long, timestamp: Long): LastUpdate = new LastUpdate(resourceName[T], lastId, timestamp)

}