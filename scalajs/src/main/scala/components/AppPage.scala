package components

import scala.util.Random
import scala.language.existentials
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.util.{Failure, Random, Success}
import scala.language.existentials
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.typedarray._
import org.scalajs.dom.ext.AjaxException
import dom.ext.Ajax
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shared.Keys
import client.User
import diode.Action
import diode.react.ModelProxy
import services.MegaContent
import services.UseLocalStorageUser
import client.Main.Loc

// Translation of App
object AppPage {

  case class Props(ctl: RouterCtl[Loc], proxy: ModelProxy[MegaContent], userId: Option[String], showAddFriends: Boolean)


  protected class Backend($: BackendScope[Props, Unit]) {

    def willReceiveProps(currentProps: Props, nextProps: Props): Callback = {
      println("AppPage | willReceiveProps")
      Callback.empty
    }

    def mounted(p: Props): japgolly.scalajs.react.Callback = {
      println("AppPage | mounted")
      p.proxy.dispatchCB(UseLocalStorageUser)
    }




    def render(props: Props): VdomElement = {
      println("render | AppPage")
      if (props.proxy.value.userLogin.loginChecked) {
        println("render | AppPage | login checked")
        val userOpt = props.proxy.value.userLogin.loggedUser
        userOpt match {
          case Some(user) => {
            // set UserChirps if userID
            // set AddFriendPage if showAddFriends
            val subComponent = if (props.showAddFriends) {
              AddFriendPage(props.ctl)
            } else {
              props.userId match {
                case Some(uid) =>
                  UserChirps(props.ctl,props.proxy, uid)
                case None =>
                  ActivityStream(props.ctl, props.proxy, user)
              }
            }
            // user is logged
            PageLayout(props.ctl, props.proxy,
              subComponent
            )
          }
          case None =>  {
            PageLayout(props.ctl, props.proxy,
              ContentLayout("Login",
                LoginForm(props.proxy)
              )
            )
          }
        }
      } else {
        println("render | AppPage | loading")
        <.div(^.className :="loading")
      }
    }
  }
  // create the React component for Dashboard
  private val component = ScalaComponent.builder[Props]("AppPage")
    .renderBackend[Backend]
    .componentWillReceiveProps(scope => scope.backend.willReceiveProps(scope.currentProps, scope.nextProps))
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

  def apply(ctl: RouterCtl[Loc], proxy: ModelProxy[MegaContent], userId: Option[String], showAddFriends: Boolean) = {
    println("AppPage | apply")
    component(Props(ctl, proxy, userId, showAddFriends))
  }
}
