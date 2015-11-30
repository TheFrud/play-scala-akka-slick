package actors

import akka.actor.{Props, PoisonPill, Actor, ActorRef}

import scala.collection.mutable.ListBuffer

/**
 * Created by Fredrik on 25-Sep-15.
 */

object EchoActor{

  def props = Props[EchoActor]
  var echoActors: List[ActorRef] = Nil

}

class EchoActor(out: ActorRef) extends Actor {

  var num = 0

  def receive = {
    case s: String =>
      println("got something");
      num = num + 1;
      out ! s + " : " + num

  }

}
