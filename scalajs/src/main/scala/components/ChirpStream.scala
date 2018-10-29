package components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.extra._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import services.StreamUtils.Socket
import services.{MegaContent, RootModel, StreamUtils, UserUtils}
import shared.{Keys, PostedMessage}
import upickle.default._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import client.User

import scala.language.existentials
import scala.scalajs.js.JSON
import client.Main.Loc
import client.Main.LoginLoc
import diode.react.ModelProxy
import japgolly.scalajs.react.extra.Reusability
import org.scalajs.dom
//import components.{ContentLayout, PageLayout}
import dom.ext.Ajax
import upickle.default._
import upickle.default.{macroRW, ReadWriter => RW}
import scala.concurrent.ExecutionContext.Implicits.global
import shared.Keys

import scala.concurrent.ExecutionContext.Implicits.global

object ChirpStream {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent], users: List[String])
  case class State(message: Option[String])
  //val stateReuse: Reusability[State] = Reusability.byRefOr_==
  //val propsReuse: Reusability[Props] = Reusability.byRefOr_==


  protected class Backend($: BackendScope[Props, State]) {


    def willReceiveProps(currentProps: Props, nextProps: Props): Callback = {
      println("ChirpStream | will receive props")
      Callback.empty
    }

    def mounted(p: Props, s: State): japgolly.scalajs.react.Callback = {
      println("ChirpStream mounted")
      Callback.empty
    }

    def unmount(p: Props): japgolly.scalajs.react.Callback = {
      println("ChirpStream unmount")
      Callback.empty
    }

    // user has entered the user Id and submit it
    def handleSubmit(p: Props, s: State, e: ReactEventFromInput): Callback = {
      e.preventDefaultCB >> {
        s.message match {
          case Some(text) =>
            val trimText: String =  text.trim()
            if (trimText.length > 0) {
              val userId = dom.window.localStorage.getItem(Keys.userIdKey)
              val postedMessage = PostedMessage(userId,trimText)
              val postUrl = "/api/chirps/live/" + userId
              val request = Ajax.post(
                url = postUrl,
                data = write(postedMessage)
              ).recover {
                // Recover from a failed error code into a successful future
                case dom.ext.AjaxException(req) => req
              }.map( r =>
                r.status match {
                  case 200 =>
                    $.modState(_.copy(message = None))
                  case _ =>
                    println("ChirpForm | url " +  postUrl + " error: " + r.status + " with message: " + r.responseText)
                    Callback.empty
                }
              )
              Callback.future(request)
            } else Callback.empty

          case None =>
            Callback.empty
        }
      }
    }

    def handleMessageChange(e: ReactEventFromInput) = {
      val newMessage = if (e.target.value == null) None else Some(e.target.value)
      $.modState(_.copy(message = newMessage))
    }


    def render(p: Props, s: State): VdomElement = {
      val users = p.users.map( userId => p.proxy.value.users(userId))
      val chirps = client.Chirp.orderedChirpsOfUsers(users)
      println("ChirpStream | render with " + chirps)
      <.div(^.className := "chirpStream",
        <.hr(),
        chirps toTagMod (
          chirp => {
            val userName = p.proxy.value.users(chirp.userId).name
            Chirp(p.router, chirp.userId, userName, chirp.uuid, chirp.message)
          }
        )
      )
    }
  }

  /*implicit val picReuse   = Reusability.by((_: State).chirps)  // ← only check id
  val stateReuse: Reusability[State] = Reusability.byRefOr_==*/


  // create the React component for Dashboard
  private val component = ScalaComponent.builder[Props]("ChirpStream")
    .initialStateFromProps(p => State(None))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props, scope.state))
    .componentWillUnmount(scope => scope.backend.unmount(scope.props))
    //.configure(Reusability.shouldComponentUpdate(propsReuse,stateReuse))
    .componentWillReceiveProps(scope => scope.backend.willReceiveProps(scope.currentProps, scope.nextProps))
    .build

  def apply(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent], users: List[String]) = component(Props(router, proxy, users))
}
