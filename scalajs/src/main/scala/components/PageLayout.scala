package components
import client.Main._
import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._

import scala.util.Random
import scala.language.existentials
import org.scalajs.dom
import services.{AjaxClient, Logout, MegaContent, RootModel}

import scala.concurrent.ExecutionContext.Implicits.global
import shared.Keys
import dom.ext._
import org.scalajs.dom.Event

import scala.util.{Failure, Random, Success}
import scala.language.existentials
import org.scalajs.dom

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.typedarray._
import upickle.default._
import client.User
import upickle.default.{macroRW, ReadWriter => RW}
import org.scalajs.dom.ext.AjaxException
import dom.ext.Ajax
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import shared.Keys

import util._
import diode.Action
import diode.react.ModelProxy

object PageLayout {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent])


    // create the React component for Dashboard
  private val component = ScalaComponent.builder[Props]("PageLayout")
    .renderPC((_, props, c) => {
      val loggedUserIdOpt = props.proxy.value.userLogin.loggedUser
      val loggedUserOpt = if (loggedUserIdOpt.isEmpty) None else {
        val userId = loggedUserIdOpt.get
        val users = props.proxy.value.users
        if (users.contains(userId)) Some(users(userId)) else None
      }

      val showSignup = loggedUserOpt.isEmpty

      def logout(e: ReactEventFromInput): Callback = {
        e.preventDefaultCB >> {
          dom.window.localStorage.removeItem(Keys.userIdKey)
          props.proxy.dispatchCB(Logout)
        }
      }


      val button: VdomElement = loggedUserOpt match  {
        case Some(user) => <.a(^.className := "btn", ^.href :="#", ^.onClick ==> logout, "Logout")
        case None => if (showSignup) {
          props.router.link(SignupLoc)("Sign up", ^.className := "btn")
        } else {
          props.router.link(LoginLoc)("Login", ^.className := "btn")
        }
      }
      val links: VdomElement = loggedUserOpt match  {
        case Some(user) => <.div(^.className := "tertiary-nav",
          props.router.link(AddFriendLoc)("Add Friend"),
          props.router.link(LoginLoc)("Feed"),
          props.router.link(UserChirpLoc(user.userId))(user.name)
        )
        case None =>
          <.div("")

      }

      <.div(^.id := "clipped",
        <.div(^.id := "site-header",
          <.div(^.className := "row",
            <.div(^.className := "small-3 columns",props.router.link(LoginLoc)("Chirper", ^.id := "logo")),
            <.div(^.className := "small-9 columns",
              <.nav(
                <.div(^.className := "tertiary-nav",
                  links
                ),
                <.div(^.className := "primary-nav",
                  button
                )
              )
            )
          )
        ),
        c
      )
    }
    ).build



  def apply(router: RouterCtl[Loc], proxy: ModelProxy[MegaContent], children: VdomNode*) =
    component(Props(router, proxy))(children: _*)
}
