package actors

import akka.actor.Actor

/**
 * Created by Fredrik on 12-Oct-15.
 */
class ChatUser extends Actor{

  def receive = {
    case s: String => println(s)
  }

}
