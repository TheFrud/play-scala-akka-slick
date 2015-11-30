package controllers

import actors._
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import akka.actor._
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import slick.backend.DatabasePublisher
// import slick.driver.H2Driver.api._
import slick.driver.PostgresDriver.api._
import models.Tables._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.inject.ApplicationLifecycle
import scala.util.{Success, Failure}

object Application{
  // AKKA
  val system = ActorSystem("actorsystem")
  val myactor = system.actorOf(Props(new MyActor), "myactor")
}

class Application extends Controller {

  implicit val timeout = Timeout(5 seconds)

  // DB
  // val db = Database.forConfig("postdb")

  // ORIGINAL FORM
  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(PersonForm.apply)(PersonForm.unapply)
  )

  // JSON SAK
  implicit val rds = (
    (__ \ 'name).read[String] and
      (__ \ 'age).read[Int]
    ) tupled

  implicit val personFormat = Json.format[Person]

  // ANDROID FORM (for login)
  val userForm = Form(
    mapping(
      "name" -> text,
      "age" -> number
    )(PersonForm.apply)(PersonForm.unapply)
  )

  // JSON SAK
  implicit val rds = (
    (__ \ 'name).read[String] and
      (__ \ 'age).read[Int]
    ) tupled

  implicit val userFormat = Json.format[Person]

  def createPerson = Action(parse.json) {implicit request =>
    /*
    val userData = userForm.bindFromRequest.get
    val person = Person(None, userData.name, userData.age)
    myactor ! userData
    */
    println("Trying to create a person")
    request.body.validate[(String, Int)].map{
      case (name, age) =>
        val person = Person(None, name, age)
        println(person)
        Application.myactor ! person
        Ok(Json.obj("answer" -> "Went well."))
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }

  }

  def index = Action.async {implicit request =>
    println("In main method")
    // val actor = system.actorOf(Props[MyActor], "actor")

    /*
    for(
      list: List[models.Tables.Person] <- ask(Application.myactor, "getall").mapTo[List[models.Tables.Person]].recover{
        case e: Exception => println("In Guardian: " + e.getMessage); List(Person(Some(0), "NoPerson", 0))
      }
    )yield Ok(views.html.index("Gick bra", list))
    */

    Application.myactor ! "Raul"

    for(
      list <- Persons.getAllNamed("Krister")
    )yield Ok(views.html.index("Ok", list))


  }
  
  def android = Action { implicit request =>
    println("Got request from android.")
    Ok("You are welcome!")
      
  }

  def getPersons = Action.async { implicit request =>
    println("Get persons function")
    val futurePersons = Persons.getAll
    futurePersons.map {persons => Ok(Json.obj("users" -> persons))}

  }

  // Websocket
  /*
  def echoWs = WebSocket.acceptWithActor[String, String] { request => out =>
    Props(classOf[EchoActor], out)
  }
  */

  def echoWs = WebSocket.acceptWithActor[String, JsValue]{
    request => out => Props(classOf[ChatSystemActor], out)
  }

  def androidWs = WebSocket.acceptWithActor[String, String]{
    request => out => Props(classOf[AndroidActor], out)
  }









}
