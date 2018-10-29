package services

import diode._
import diode.data._
import diode.util._
import diode.react.ReactConnector

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import client.{ChirpFromServer, User, UserFromServer}
import org.scalajs.dom
import shared.Keys

// Actions
case object UseLocalStorageUser extends Action
case class RegisterUser(user: UserFromServer) extends Action
case class LoginWithID(userId: String) extends Action
case class LoggedUserAgainstDB(userId: String, user: Option[UserFromServer]) extends Action
case class ProcessLocalStorageUserDBResult(user: Option[UserFromServer]) extends Action
case class RegisterFriends(friendIDs: List[String]) extends Action
case class ChirpReceived(chirp: ChirpFromServer) extends Action


case object Logout extends Action

// The base model of our application
case class UserLogin(loginChecked: Boolean = false, loggedUser: Option[User], loginError: Option[String] = None)
case class MegaContent(userLogin: UserLogin, friends: List[User])
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
    case RegisterUser(user) =>
       val u = User(user.userId,user.name,List())
       val newValue = value.copy(loginChecked = true, loggedUser = Some(u))
       val friends = user.friends
       updated(newValue, Effect.action(RegisterFriends(friends)))


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

  }
}
class FriendsHandler[M](modelRW: ModelRW[M, List[User]]) extends ActionHandler(modelRW) {
  override def handle = {
    case RegisterFriends(friendIDs) =>
      noChange
  }
}

// Application circuit
object SPACircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  // initial application model
  override protected def initialModel = RootModel(MegaContent(UserLogin(false,None), List()))
  // combine all handlers into one
  override protected val actionHandler = composeHandlers(
    new UserLoginHandler(zoomTo(_.content.userLogin)),
    new FriendsHandler(zoomTo(_.content.friends))
  )
}