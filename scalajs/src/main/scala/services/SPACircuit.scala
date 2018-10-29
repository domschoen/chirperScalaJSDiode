package services

import client.Main.LoginLoc
import diode._
import diode.data._
import diode.util._
import diode.react.ReactConnector

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import client.{Chirp, ChirpFromServer, User, UserFromServer}
import org.scalajs.dom
import shared.Keys

import scala.concurrent.Future

// Actions
case object UseLocalStorageUser extends Action
case class RegisterUser(user: UserFromServer) extends Action
case class LoginWithID(userId: String) extends Action
case class LoggedUserAgainstDB(userId: String, user: Option[UserFromServer]) extends Action
case class ProcessLocalStorageUserDBResult(user: Option[UserFromServer]) extends Action
case class RegisterFriends(friendIDs: List[String]) extends Action
case class ChirpReceived(chirp: ChirpFromServer) extends Action
case class MonitorUser(userId: String) extends Action
case class GetMonitoredUser(userId: String) extends Action
case class RegisterInUsers(userId: String, user: Option[UserFromServer]) extends Action
case class RegisterUsersInUsers(userId: String, users: List[Option[UserFromServer]]) extends Action
case class RegisterFriend(userId: String, friendId:String, users: Option[UserFromServer]) extends Action

case class FetchFriends(userId: String) extends Action

case class StartStreamForUser(userId: String) extends Action
case class StartStreamsForUsers(users: List[User]) extends Action

case class AddFriend(userId: String) extends Action
case class AddFriendToUser(friendId: String, userId: String) extends Action

case class FetchUsersWithUserIds(userIds: List[String]) extends Action


case object Logout extends Action

// The base model of our application
case class UserLogin(loginChecked: Boolean = false, loggedUser: Option[String], loginError: Option[String] = None, friendsFetched: Boolean = false)
case class MegaContent(userLogin: UserLogin, users: Map[String, User])
case class RootModel(content: MegaContent)


/**
  * Handles actions
  *
  * @param modelRW Reader/Writer to access the model
  */
class UserLoginHandler[M](modelRW: ModelRW[M, UserLogin]) extends ActionHandler(modelRW) {
  override def handle = {
    case UseLocalStorageUser =>
      println("UserLoginHandler | UseLocalStorageUser")
      val uid = dom.window.localStorage.getItem(Keys.userIdKey)
      val userIdOpt = if (uid == null) None else Some(uid)
      println("UserLoginHandler | UseLocalStorageUser | userIdOpt " + userIdOpt)

      userIdOpt match {
        case Some(userId) =>
          effectOnly(Effect(UserUtils.getUser(userId).map(ProcessLocalStorageUserDBResult(_))))
        case None =>
          effectOnly(Effect.action(ProcessLocalStorageUserDBResult(None)))
      }


    case ProcessLocalStorageUserDBResult(userOpt) =>
      userOpt match {
        case Some(user) =>
          effectOnly(Effect.action(RegisterUser(user)))

        case None =>
          dom.window.localStorage.removeItem(Keys.userIdKey)
          println("UserLoginHandler | ProcessLocalStorageUserDBResult | None ")
          val newValue = value.copy(loginChecked = true, loggedUser = None)
          updated(newValue)
      }


    // Could be:
    // - a user retrieved from id in localStorage
    // - a user logged
    case RegisterUser(userFromServer) =>
       val newValue = value.copy(loginChecked = true, loggedUser = Some(userFromServer.userId))
       updated(newValue, Effect.action(RegisterInUsers(userFromServer.userId, Some(userFromServer))))


    case LoggedUserAgainstDB(userId, userOpt) =>
      userOpt match {
        case Some(user) =>
          dom.window.localStorage.setItem(Keys.userIdKey, user.userId)
          effectOnly(Effect.action(RegisterUser(user)))

        case None =>
          val errorMsg = "User " + userId + " does not exist."
          println("User not found " + errorMsg)
          val newValue = value.copy(loginError = Some(errorMsg))
          updated(newValue)
      }

    case LoginWithID(userId) =>
      effectOnly(Effect(UserUtils.getUser(userId).map(LoggedUserAgainstDB(userId, _))))

    case GetMonitoredUser(userId) =>
      println("UserLoginHandler | GetMonitoredUser | userId " + userId)
      effectOnly(Effect(UserUtils.getUser(userId).map(RegisterInUsers(userId, _))))

    case AddFriend(friendId) =>
      println("UserLoginHandler | AddFriend | friendId " + friendId)
      effectOnly(Effect.action(AddFriendToUser(friendId, value.loggedUser.get)))

    case AddFriendToUser(friendId,userId) =>
      println("UserLoginHandler | AddFriendToUser | friendId " + friendId + " userId " + userId)
      effectOnly(Effect(UserUtils.getUser(friendId).map(RegisterFriend(userId, friendId, _))))


  }
}

class UsersHandler[M](modelRW: ModelRW[M, Map[String, User]]) extends ActionHandler(modelRW) {
  override def handle = {




    case ChirpReceived(chirp) =>
      println("UsersHandler | ChirpReceived | chirp " + chirp)
      val userId = chirp.userId
      if (value.contains(userId)) {
        println("UsersHandler | ChirpReceived | found existing userId  " + userId)
        val user = value(userId)
        println("UsersHandler | ChirpReceived | user  " + user)
        val newChirp = Chirp.chirpWithDataFromServer(chirp)
        val newChirps = newChirp :: user.chirps
        val updatedUser = user.copy(chirps = newChirps)
        val newValue = value + (userId -> updatedUser)
        println("UsersHandler | ChirpReceived | newValue  " + newValue)

        updated(newValue)
      } else {
        // should never happen
        noChange
      }
    case MonitorUser(userId) =>
      effectOnly(Effect.action(GetMonitoredUser(userId)))

    case RegisterFriend(userId, friendId, userFromServerOpt) =>
      println("UsersHandler | RegisterFriend | userId " + userId + " friendId " + friendId + " userFromServerOpt " + userFromServerOpt)
      userFromServerOpt match {
        case Some(userFromServer) =>
          val loggedUser = value(userId)

          val updatedUser = loggedUser.copy(friends = friendId :: loggedUser.friends)
          val newValue = value + (userId -> updatedUser)

          updated(newValue, Effect.action(RegisterInUsers(friendId, userFromServerOpt)))
        case None =>
          noChange
      }




    case RegisterInUsers(userId, userFromServerOpt) =>
      println("UsersHandler | RegisterInUsers | userId " + userId + " server response " + userFromServerOpt)
      userFromServerOpt match {
        case Some(uerFromServer) =>
          val newUser = User.userWithDataFromServer(uerFromServer)
          val newValue = value + (userId -> newUser)
          updated(newValue, Effect.action(StartStreamForUser(userId)))

        case None =>
          // TODO should we care ?
          noChange
      }

    case StartStreamForUser(userId) =>
      println("UsersHandler | StartStreamForUser | " + userId)
      StreamUtils.createUserStream(userId)
      noChange


    case FetchFriends(userId) =>
      println("UserLoginHandler | FetchFriends | userId " + userId)
      val loggedUser = value(userId)
      val futureFriends = Future.sequence(loggedUser.friends.map(friendId => UserUtils.getUser(friendId)))
      effectOnly(Effect(futureFriends.map(RegisterUsersInUsers(userId, _))))


    case RegisterUsersInUsers(userId, users: List[Option[UserFromServer]]) =>
      val newUserOpts = users.map( u => {
        u match {
          case Some(uerFromServer) =>
            Some(User.userWithDataFromServer(uerFromServer))
          case None =>
            // TODO should we care ?
            None
        }
      })
      val newUsers = newUserOpts.flatten
      val newUsersMap = newUsers.map(u => (u.userId, u)).toMap
      val newValue = value ++ newUsersMap
      updated(newValue, Effect.action(StartStreamsForUsers(newUsers)))

    case StartStreamsForUsers(users: List[User]) =>
      users.map(u => {
        StreamUtils.createUserStream(u.userId)
      })
      noChange



  }
}

// Application circuit
object SPACircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  // initial application model
  override protected def initialModel = RootModel(MegaContent(UserLogin(false,None), Map()))
  // combine all handlers into one
  override protected val actionHandler = composeHandlers(
    new UserLoginHandler(zoomTo(_.content.userLogin)),
    new UsersHandler(zoomTo(_.content.users))
  )
}