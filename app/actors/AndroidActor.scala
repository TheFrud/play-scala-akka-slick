package actors

import akka.actor.{Actor, ActorRef, Props}

/**
 * Created by Fredrik on 13-Nov-15.
 */
object AndroidActor{

  var lamp: Boolean = false
  var agents: List[ActorRef] = Nil

  def props = Props[EchoActor]

}

class AndroidActor(out: ActorRef) extends Actor {

  var num = 0

  println("Trying to add user to agents.")
  println("Number of agents: " + AndroidActor.agents.size)
  println("Sending lamp state")

  if(!isReceipient){
    AndroidActor.agents = out :: AndroidActor.agents
    // sendToAll(Message.joined)
    // sendCurrentChatters
  }
  else{
    println("User already joined.")
  }

  // Sending lamp state
  // sendLampState

  def isReceipient = {
    val isReceipient = AndroidActor.agents.exists(actor => actor == out)
    if(isReceipient){
      true
    }else {
      false
    }
  }

  def sendToAll(message: String) = {
    for(o <- AndroidActor.agents){
      o ! message
    }
  }

  def sendLampState = {
    if(checkLampState()){
      out ! "ON"
    } else {
      out ! "OFF"
    }
  }

  def checkLampState(): Boolean ={
    AndroidActor.lamp
  }

  def receive = {
    case s: String if s.equals("Android") =>
      num = num + 1
      val m = "Got specific android message from you! " + num + " times have this occured.";
      println(m)
      out ! m

    case s: String if s.equals("ON")=>
      AndroidActor.lamp = true
      // Även lägga in vem som tände lampan
      val m = "ON"
      println(m)
      sendToAll(m)

    case s: String if s.equals("OFF")=>
      AndroidActor.lamp = false
      // Även lägga in vem som tände lampan
      val m = "OFF"
      println(m)
      sendToAll(m)

    case s: String if s.equals("LEAVE") =>
      removeFromAgents

    case s: String if s.equals("STATE_REQUEST") =>
      sendLampState

    case s: String =>
      val m = "Got something";
      println(m)
      out ! m

  }
  def removeFromAgents = {
    val newList = AndroidActor.agents.filterNot(actor => actor == out)
    AndroidActor.agents = newList
  }

  override def postStop() = {
    removeFromAgents
    println("User removed from agents.")
    // sendToAll(Message.left)
  }
}
