package components

import client.Main.Loc
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import services.{MegaContent, MonitorUser, StreamUtils, UserUtils}
import shared.Keys
import client.User
import diode.react.ModelProxy
import diode.Action

import scala.concurrent.ExecutionContext.Implicits.global

object UserChirps {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent],userId: String)

  // TODO :  notFound shoud be added because at the beginning is shouldn't be not found even if user is not existing
  case class State(user: Option[User], notFound : Boolean)


  protected class Backend($: BackendScope[Props, State]) {
    def mounted(p: Props): japgolly.scalajs.react.Callback = {
      println("UserChirps mounted")

      // Create the user and the WS if the user doesn't exists
      val userExists = p.proxy.value.users.contains(p.userId)
      Callback.when(!userExists)(p.proxy.dispatchCB(MonitorUser(p.userId)))
    }

    def render(props: Props, s: State): VdomElement = {
      val userId = props.userId
      if (s.notFound) {
        <.div(^.className :="userChirps",
          <.h1("User " + userId + " not found")
        )
      } else {
        val userName = s.user match {
          case Some(user) =>
            user.name
          case None =>
            userId
        }
        val storageUserId = dom.window.localStorage.getItem(Keys.userIdKey)
        val showChirpForm = storageUserId == userId

        ContentLayout("Chirps for " + userName,
              Section(
                <.div(^.className := "small-12 columns",
                  ChirpForm().when(showChirpForm),
                  ChirpStream(props.router, props.proxy, List(userId))
                )
              )
            )
        }

      }

    }

    // create the React component for Dashboard
  private val component = ScalaComponent.builder[Props]("UserChirps")
    .initialState(State(None, false))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

  def apply(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent], userId: String) = component(Props(router, proxy, userId))
}
