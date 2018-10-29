package client

import java.util.UUID
import upickle.default.macroRW
import upickle.default.{macroRW, ReadWriter => RW}
import com.zoepepper.facades.jsjoda._


case class UserFromServer(userId: String, name: String , friends: List[String])
object UserFromServer{
  implicit def rw: RW[UserFromServer] = macroRW
}


case class User(userId: String, name: String, friends: List[String], chirps: List[Chirp], observed: Boolean = false)
object User {
  def userWithDataFromServer(userFromServer : UserFromServer) = {
    User(userFromServer.userId, userFromServer.name, userFromServer.friends, List())
  }


}


// Chirp message receive with WS:
// {"userId":"Jean","message":"This is my new message","timestamp":1539760786.932000000,"uuid":"138036c1-97ab-4dac-a6b2-3fa8f3572c57"}	1539760894.5932655

case class Chirp(userId: String, message: String, timestamp: Instant, uuid: UUID)


case class ChirpFromServer(userId: String, message: String, timestamp: Double , uuid: UUID)
object ChirpFromServer{
  implicit def rw: RW[ChirpFromServer] = macroRW
}


object Chirp {
  def chirpWithDataFromServer(serverChirp : ChirpFromServer) = {
    // TODO use data from server to create the Instant object instead of creating a now
    Chirp(serverChirp.userId, serverChirp.message, Instant.now(), serverChirp.uuid)
  }

  def orderedChirpsOfUsers(users: List[User]) : List[Chirp] = {
    // TODO order the chirp by timestamp
    users.flatMap(u => u.chirps)
  }
}