package actors

import akka.actor.{PoisonPill, ActorLogging, Actor}
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

    case Person(None, name, age) =>
      Persons.create(Person(None, name, age))

    case GetAll =>
      val futurePersons: Future[List[models.Tables.Person]] = Persons.getAll
      futurePersons.map{f => f}

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
