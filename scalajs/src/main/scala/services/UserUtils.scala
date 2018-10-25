package services

import japgolly.scalajs.react.Callback
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import upickle.default.read

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import upickle.default._
import client.{User, UserFromServer}
import upickle.default.{macroRW, ReadWriter => RW}

object UserUtils {



  def getUser(userId: String) : Future[Option[User]] = {
    Ajax.get("/api/users/" + userId).recover {
      // Recover from a failed error code into a successful future
      case dom.ext.AjaxException(req) => req
    }.map( r =>
      r.status match {
        case 200 =>
          val user = read[UserFromServer](r.responseText)
          Some(User())
        case _ =>
          None
      }
    )
  }

}
