package actors

import akka.actor.{PoisonPill, ActorLogging, Actor}
import play.api.libs.json.Json
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import slick.backend.DatabasePublisher
// import slick.driver.H2Driver.api._
import slick.driver.PostgresDriver.api._
import models.Tables._
/**
 * Created by Fredrik on 11-Oct-15.
 */
case object GetAll

class MyActor extends Actor with ActorLogging{

  var names: List[String] = Nil

    override def preStart{
      println("Pre start method.")
    }

    def receive = {

    /*
    case User(None, name, password) =>
      val user = User(None, name, password)
      val futureExist = Users.login(user)
      futureExist.map(v =>
        v match {
          case l: List[User] if l.size > 0 =>
            val user = l.head
            Ok(Json.obj("status" -> "Success", "userId" -> user.id, "userName" -> user.name, "userPassword" -> user.password))

          case _ => Users.create(User(None, name, password))
        }
      */

    case GetAll =>
      val futureUsers: Future[List[models.Tables.User]] = Users.getAll
      futureUsers.map{f => f}

    /*
    case "getall" =>
      val futurePersons = Future {
        println("In the future")
        List(Person(Some(0), "Frans", 23), Person(Some(1), "Olle", 65))
      }

      futurePersons foreach {println}

      val allPersons = for(
        list <- futurePersons.map(list => list)
      )yield list

      allPersons.foreach {println}

      println(sender)
      allPersons.onSuccess{
        case list => sender ! list; println(sender());println("END---")
        case _ => // println("Success")
      }

      allPersons.onFailure {
        case f => println("In actor: " + f.getMessage)
        case _ => println("Failure")
      }
    */
    case s: String => println("Message received!"); names = s :: names; for(n <- names){println(n)}

    case _ => println("Nothing!")

  }



}
