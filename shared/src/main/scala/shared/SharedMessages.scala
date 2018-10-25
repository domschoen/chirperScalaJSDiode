package shared


import java.util.UUID

import upickle.default.{macroRW, ReadWriter => RW}


object Keys  {
  val userIdKey = "userId"
}



case class PostedMessage(userId: String, message: String)
object PostedMessage{
  implicit def rw: RW[PostedMessage] = macroRW
}


case class StreamForUsers(userIds: List[String])
object StreamForUsers{
  implicit def rw: RW[StreamForUsers] = macroRW
}
