package actors

import com.fasterxml.jackson.annotation.JsonValue

import scala.util.Success
import scala.util.Failure
import akka.actor.{ActorRef, Props, Actor}
import java.time._
import java.util.Date
import java.text.{SimpleDateFormat, DateFormat}
import akka.util.Timeout
import play.api.libs.json._
import play.api.libs.json.{JsNull,Json,JsString,JsValue}
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import scala.util.matching.Regex
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Created by Fredrik on 12-Oct-15.
 */
object ChatSystemActor{

  var chatters: List[ActorRef] = Nil
  def props(out: ActorRef) = {
    Props(new ChatSystemActor(out))
  }

  // def props = Props[ChatSystemActor]

}

case class Chatter(var name: String, var notUsed: Int)

class ChatSystemActor(out: ActorRef) extends Actor{

  println("Someone connected to the web socket.")

  // JSON SAK
  implicit val rds = (
    (__ \ 'name).read[String] and
      (__ \ 'notUsed).read[Int]
    ) tupled

  implicit val actorFormat = Json.format[Chatter]

  implicit val timeout = Timeout(5 seconds)
  var name: String = "Anonymous"
  var notUsed: Int = 0

  // PREFORMATTED MESSAGES
  object Message{

    def joined = {
      val joined = " has joined the chat!"
      time + nameFormatted + joined
    }

    def left = {
      val left = " has left the chat"
      time + nameFormatted + left
    }

    def ordinary(message: String) = {
      time + nameFormatted + " " + message
    }

    def changedName(newName: String) = {
      val changed = " has changed name to "
      time + name + changed + newName
    }

    def haveToJoin = {
      "You have to join the chat to be able to send messages!"
    }

  }
  def isReceipient = {
    val isReceipient = ChatSystemActor.chatters.exists(actor => actor == out)
    if(isReceipient){
      true
    }else {
      false
    }
  }

  def removeFromChat = {
    val newList = ChatSystemActor.chatters.filterNot(actor => actor == out)
    ChatSystemActor.chatters = newList
  }

  def time = {
    val time = new Date()
    // val sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss")
    val sdf = new SimpleDateFormat("hh:mm:ss")
    val formattedTime = sdf.format(time)
    formattedTime + ": "
  }

  def nameFormatted = {
    name + ": "
  }

  // Convert string message to Json message
  def messageToJson(message: String): JsValue =  {
    val json: JsValue = Json.obj(
      "message" -> message
    )
    json
  }

  def sendToAll(message: String) = {
    val messageInJson = messageToJson(message)
    for(o <- ChatSystemActor.chatters){
      o ! messageInJson
    }
  }

  def sendToAll(chatters: JsValue) = {
    for(o <- ChatSystemActor.chatters){
      o ! chatters
    }
  }

  def sendToSelf(message: String) = {
    val messageInJson = messageToJson(message)
    out ! messageInJson
  }

  def sendToSelf(chatters: JsValue): Unit ={
    out ! chatters
  }

  def sendCurrentChatters = {
    // Send to joined person
    val currentChatters = ChatSystemActor.chatters.map(c => c.toString)
    val currentChattersJson: List[JsObject] = currentChatters.map(c => Json.obj("chatter" -> c))
    val properJson = Json.toJson(currentChattersJson)
    val jsonHolder = Json.obj("chatters" -> properJson)
    sendToAll(jsonHolder)

  }

  def receive = {
    case s: String if s.contains("Change Name") =>
      println("Trying to change actor's name")
      val newName = s.substring(13)
      val message = Message.changedName(newName)
      name = newName
      sendToAll(message)

    case s: String if s.equals("Join") =>
      println("Trying to add actor to recepients.")
      if(!isReceipient){
        ChatSystemActor.chatters = out :: ChatSystemActor.chatters
        sendToAll(Message.joined)
        sendCurrentChatters
      }
      else{
        println("User already joined.")
      }

    case s: String if s.equals("Leave") =>
      println("Try to remove actor from recepients.")
      removeFromChat
      sendToAll(Message.left)

    case s: String if s.equals("Android") =>
      println("Message from android")

    case s: String =>
      println("Got message from a user.")
      println("Receipients: " + ChatSystemActor.chatters)
      if(isReceipient){
        sendToAll(Message.ordinary(s))
      }else {
        sendToSelf(Message.haveToJoin)
        println(Message.haveToJoin)
      }


  }

  override def postStop() = {
    removeFromChat
    sendToAll(Message.left)
  }

}

