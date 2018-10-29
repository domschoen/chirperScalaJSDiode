package components

import client.Main.Loc
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import services._

import scala.util.Random
import scala.language.existentials
import shared.Keys
import client.User
import components.UserChirps.Props
import diode.react.ModelProxy

object ActivityStream {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent], userId: String)
  case class State(users: Map[String, User])


  protected class Backend($: BackendScope[Props, State]) {
    def mounted(p: Props): japgolly.scalajs.react.Callback = {
      println("ActivityStream | mounted")

      // Create the user and the WS if the user doesn't exists
      val friendsFetched = p.proxy.value.userLogin.friendsFetched
      Callback.when(!friendsFetched)(p.proxy.dispatchCB(FetchFriends(p.userId)))
    }


    def render(p: Props, s: State): VdomElement = {
      val users = p.proxy.value.users
      if (users.contains(p.userId)) {
        val showUsers = p.userId :: users(p.userId).friends

        ContentLayout("Chirps feed",
          Section(
            <.div(^.className := "small-12 columns",
              ChirpForm(),
              ChirpStream(p.router,  p.proxy, showUsers)
            )
          )
        )

      } else {
        <.div(^.className :="loading")
      }
    }
  }
    // create the React component for Dashboard
  private val component = ScalaComponent.builder[Props]("ActivityStream")
    .initialState(State(Map()))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

  def apply(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent],userId: String) = component(Props(router,proxy, userId))
}
