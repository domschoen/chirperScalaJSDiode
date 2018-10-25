package services

import diode._
import diode.data._
import diode.util._
import diode.react.ReactConnector

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import client.User
import org.scalajs.dom
import shared.Keys

// Actions
case class CheckUser(userIDFromStorage: Option[String]) extends Action
case class RegisterUser(user: Option[User]) extends Action

// The base model of our application
case class UserLogin(loginChecked: Boolean = false, loggedUser: Option[User])
case class RootModel(userLogin: UserLogin, friends: List[User])


/**
  * Handles actions
  *
  * @param modelRW Reader/Writer to access the model
  */
class UserLoginHandler[M](modelRW: ModelRW[M, UserLogin]) extends ActionHandler(modelRW) {
  override def handle = {
    case CheckUser(userIDFromStorage) =>
      userIDFromStorage match {
        case Some(userId) =>
          effectOnly(Effect(UserUtils.getUser(userId).map(RegisterUser(_))))
        case None =>
          effectOnly(Effect.action(RegisterUser(None)))
      }

    case RegisterUser(userOpt) =>
      userOpt match {
        case Some(user) =>
          updated(UserLogin(loginChecked = true, loggedUser = Some(user)))
        case None =>
          dom.window.localStorage.removeItem(Keys.userIdKey)
          updated(UserLogin(loginChecked = true, loggedUser = None))
      }
  }
}


// Application circuit
object SPACircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  // initial application model
  override protected def initialModel = RootModel(UserLogin(false,None), List())
  // combine all handlers into one
  override protected val actionHandler = composeHandlers(
    new UserLoginHandler(zoomTo(_.userLogin))
  )
}