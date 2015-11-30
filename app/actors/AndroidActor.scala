package actors

import akka.actor.{Actor, ActorRef, Props}

/**
 * Created by Fredrik on 13-Nov-15.
 */
object AndroidActor{

  def props = Props[EchoActor]

}

class AndroidActor(out: ActorRef) extends Actor {

  var num = 0

  def receive = {
    case s: String if s.equals("Android") =>
      num = num + 1
      val m = "Got specific android message from you! " + num + " times have this occured.";
      println(m)
      out ! m

    case s: String =>
      val m = "Got something";
      println(m)
      out ! m
  }

}
