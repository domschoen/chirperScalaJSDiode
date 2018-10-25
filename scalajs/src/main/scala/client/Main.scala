package client

import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import components.{AddFriendPage, AppPage, SignUpPage}
import com.zoepepper.facades.jsjoda._
import services.SPACircuit
import diode.react.ModelProxy

@JSExportTopLevel("Main")
object Main extends js.JSApp {

  sealed trait Loc
  case object LoginLoc extends Loc
  case object SignupLoc extends Loc
  case object AddFriendLoc extends Loc
  case class UserChirpLoc(userId: String) extends Loc


  // configure the router
  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
    import dsl._
    val wrapper = SPACircuit.connect(_.content)

    (emptyRule
      | staticRoute(root, LoginLoc) ~> renderR(ctl => SPACircuit.wrap(_.content)(proxy => AppPage(ctl, proxy, None, false)))
      | staticRoute("#signup", SignupLoc) ~> renderR(ctl => SPACircuit.wrap(_.content)(proxy => SignUpPage(ctl,proxy)))
      | staticRoute("#addFriend", AddFriendLoc) ~> renderR(ctl => SPACircuit.wrap(_.content)(proxy => AppPage(ctl, proxy, None, true)))
      | dynamicRouteCT("#users" / string(".*").caseClass[UserChirpLoc]) ~> dynRenderR(
        (m, ctl) => {
          wrapper(p => AppPage(ctl, p, Some(m.userId), false))
        })
    ).notFound(redirectToPage(LoginLoc)(Redirect.Replace))

  }


  def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
    <.div(^.id := "clipped",
      <.div(^.id := "site-header",
        <.div(^.className := "row",
          <.div(^.className := "small-3 columns",c.link(LoginLoc)("Chirper", ^.id := "logo")),
          <.div(^.className := "small-9 columns",
            <.nav(
              <.div(^.className := "tertiary-nav",
                <.div(^.className := "tertiary-nav")
              ),
              <.div(^.className := "primary-nav",
                //Button(Button.Props(proxy.dispatchCB(UpdateMotd()), CommonStyle.danger), Icon.refresh, " Login")
                "ButtonLogin"
              )
            )
          )
        )
      ),
      <.div(^.className := "container", r.render())
    )
  }

  @JSExport
  def main(): Unit = {
    println("Welcome to your Play application's JavaScript!");
    val someInstant = Instant.ofEpochMilli(1540222606472d)
    val someDate = LocalDateTime.ofInstant(someInstant, ZoneId.of("GMT+2"));
    println("someInstant" + someDate.toString)

    val router = Router(BaseUrl.until_#, routerConfig)

    router().renderIntoDOM(dom.document.getElementById("root"))
  }


}
