package dataswift.challenge

import io.circe.{Decoder, Encoder}

import scala.reflect.ClassTag

/*
{
    "login": "ezmobius",
    "id": 5,
    "node_id": "MDQ6VXNlcjU=",
    "avatar_url": "https://avatars0.githubusercontent.com/u/5?v=4",
    "gravatar_id": "",
    "url": "https://api.github.com/users/ezmobius",
    "html_url": "https://github.com/ezmobius",
    "followers_url": "https://api.github.com/users/ezmobius/followers",
    "following_url": "https://api.github.com/users/ezmobius/following{/other_user}",
    "gists_url": "https://api.github.com/users/ezmobius/gists{/gist_id}",
    "starred_url": "https://api.github.com/users/ezmobius/starred{/owner}{/repo}",
    "subscriptions_url": "https://api.github.com/users/ezmobius/subscriptions",
    "organizations_url": "https://api.github.com/users/ezmobius/orgs",
    "repos_url": "https://api.github.com/users/ezmobius/repos",
    "events_url": "https://api.github.com/users/ezmobius/events{/privacy}",
    "received_events_url": "https://api.github.com/users/ezmobius/received_events",
    "type": "User",
    "site_admin": false
  }

 */
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