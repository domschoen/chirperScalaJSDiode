package services

import japgolly.scalajs.react.{Callback, CallbackTo}
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import org.scalajs.dom.{CloseEvent, Event, MessageEvent, WebSocket}
import shared.StreamForUsers
import upickle.default.write
import upickle.default._
import client.{Chirp, ChirpFromServer, User}
import upickle.default.{macroRW, ReadWriter => RW}

object StreamUtils {
  val baseWebsocketUrl = s"ws://${dom.document.location.host}"


  case class Socket(url: String, userId: Option[String]) {
    private val socket: WebSocket = new dom.WebSocket(url = baseWebsocketUrl + url)

    def close() = {
      socket.close()
    }

    def connect(onChirp: Chirp => Callback) {
      socket.onopen = (e: Event) => {
        userId match {
          case Some(uid) =>
            val streamForUsers = StreamForUsers(List(uid))
            val msg = write(streamForUsers)
            socket.send(msg)
          case None => ()
        }
      }
      socket.onclose = (e: CloseEvent) => {
        dom.console.log(s"Socket closed. Reason: ${e.reason} (${e.code})")
      }
      socket.onerror = (e: Event) => {
        dom.console.log(s"Socket error! ${e}")
      }
      socket.onmessage = (e: MessageEvent) => {
        val chirp = read[ChirpFromServer](e.data.toString);
        println("Socket received chirp " + chirp)
        SPACircuit.dispatch(ChirpReceived(chirp))
      }
    }
  }



  def createUserStream(userId: String):Socket = {
    Socket("/api/chirps/live", Some(userId))
  }

  def createActivityStream(userId: String)= {
    Socket("/api/activity/" + userId + "/live", None)
  }


}
