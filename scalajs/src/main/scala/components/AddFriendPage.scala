package components

import client.Main.{Loc, LoginLoc}
import components.SignUpPage.{Props, State}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^.{<, _}
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import services.{MegaContent, StreamUtils, UserUtils, AddFriend}
import shared.Keys
import client.User
import diode.react.ModelProxy
import upickle.default.write

import scala.language.existentials
import upickle.default.{macroRW, ReadWriter => RW}

import scala.concurrent.ExecutionContext.Implicits.global
import diode.Action

object AddFriendPage {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent])
  case class State(friendId: Option[String], error: Option[String])

  case class PostFriendId(friendId: String)
  object PostFriendId{
    implicit def rw: RW[PostFriendId] = macroRW
  }


  protected class Backend($: BackendScope[Props, State]) {
    def handleSubmit(p: Props, s: State, e: ReactEventFromInput): Callback = {
      e.preventDefaultCB >> {
        println("AddFriendPage | handleSubmit | s.friendId " + s.friendId)
        s.friendId match {
          case Some(id) =>
            val friendId: String =  id.trim()
            Callback.when(friendId.length > 0)(p.proxy.dispatchCB(AddFriend(friendId))) >>  p.router.set(LoginLoc)

          case _ =>
            Callback.empty
        }
      }
    }

    def handleFriendIdChange(e: ReactEventFromInput) = {
      val newValue = if (e.target.value == null) None else Some(e.target.value)
      $.modState(_.copy(friendId = newValue))
    }


    def render(props: Props, s: State): VdomElement = {
      println("AddFriendPage | render" )
      val valueString = if (s.friendId.isDefined) s.friendId.get else ""
      val errorMsg = if (s.error.isDefined) s.error.get else ""
      ContentLayout("Add friend",
        Section(
          <.div(^.className := "small-12 large-4 columns",
            <.form(^.className := "friendForm", ^.onSubmit ==> { e: ReactEventFromInput => handleSubmit(props, s, e)},
              <.input.text(^.placeholder := "Friends ID...", ^.value := valueString,
                ^.onChange ==> { e: ReactEventFromInput => handleFriendIdChange(e)}),
              {
                components.Error(errorMsg)
              }.when(s.error.isDefined),
              <.input.submit(^.value := "Add Friend")
            )
          )
        )
      )
    }
  }
    // create the React component for Dashboard
  private val component = ScalaComponent.builder[Props]("AddFriendPage")
    .initialState(State(None, None))
    .renderBackend[Backend]
    .build

  def apply(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent]) = component(Props(router, proxy))
}
