package controllers

import java.util.Date

import actors._
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json
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

  // ORIGINAL FORM
  val userForm = Form(
    mapping(
      "name" -> text,
      "password" -> text
    )(UserForm.apply)(UserForm.unapply)
  )

  // JSON SAK
  implicit val rds = (
    (__ \ 'name).read[String] and
      (__ \ 'password).read[String]
    ) tupled

  implicit val UserFormat = Json.format[User]

  // NEW FORM
  /*
  SKA IN ETT NYTT FORMULÄR HÄR!!!
   */





  def setupdb = Action {
    Users.setupdb
    Ok("DB SETUP!")
  }


  def login = Action.async(parse.json) {implicit request =>
    println("Trying to login.")

    request.body.validate[(String, String)].map{
      case (name, password) =>
        val user = User(None, name, password)
        println(new Date().getTime() + ": " + user)
        val futureExist = Users.login(user)
        futureExist.map(v =>
          v match {
            case l: List[User] if l.size > 0 =>
              val user = l.head
              Ok(Json.obj("status" -> "Success", "userId" -> user.id, "userName" -> user.name, "userPassword" -> user.password))

            case _ => Ok(Json.obj("status" -> "Failure"))
          }
          )


    }.recoverTotal{ e =>
      Future{BadRequest}
    }
  }

  def registerUser = Action(parse.json) {implicit request =>

    println("Trying to register a user")
    request.body.validate[(String, String)].map{
      case (name, password) =>
        val user = User(None, name, password)
        println(user)
        Application.myactor ! user
        // Ok("Went well.")
        Ok(Json.obj("status" -> "Success"))
    }.recoverTotal{
      e => println("Shit");Ok(Json.obj("status" -> "Failure"))
    }

  }

  def saveProfileSettings = Action.async(parse.json) {implicit request =>
    println("Save Profile Settings Method.")

    implicit val profileSettingsRead: Reads[SaveProfileForm] = (
      (JsPath \ "userId").read[Long] and
        (JsPath \ "newUserName").read[String] and
        (JsPath \ "newUserPassword").read[String]
      )(SaveProfileForm.apply _)

    request.body.validate[SaveProfileForm] match {
      case s: JsSuccess[SaveProfileForm] => {
        println(s.get)
        Users.updateUser(s.value.id, s.value.name, s.value.password)
        Future{Ok(Json.obj("status" -> "Success"))}
      }
      case e: JsError => {
        println(e)
        Future{Ok}
      }
    }

    /*
    val nameReads: Reads[Long] = (JsPath \ "userId").read[Long]
    val nameResult: JsResult[Long] = request.body.validate[Long](nameReads)
    nameResult match {
      case s: JsSuccess[Long] =>
        println("Gick bra! Värde: " + s.get)
        val users = Users.getUserById(s.get)
        users.map(list =>
          list match {
            case v: List[User] if v.size > 0 =>
              val user = v.head
              Ok(Json.obj("status" -> "Success", "userId" -> s.get, "userName" -> user.name, "userPassword" -> user.password))
            case _ => Ok(Json.obj("status" -> "Failure"))
          })


      case e: JsError => println("Gick skit")
        Future {Ok}

      case _ => Future {Ok}
    }
    */
  }



  def index = Action.async {implicit request =>
    println("In main method")
    Future{Ok("Index")}

/*
    Application.myactor ! "Raul"

    for(
      list <- Users.getAllNamed("Krister")
    )yield Ok(views.html.index("Ok", list))
*/


  }
  
  def android = Action { implicit request =>
    println("Got request from android.")
    Ok("You are welcome!")
      
  }

  def getUsers = Action.async { implicit request =>
    println("Get users function")
    val futureUsers = Users.getAll
    futureUsers.map {users => Ok(Json.obj("users" -> users))}
  }

  def getUser = Action.async(parse.json) { implicit request =>
    val nameReads: Reads[Long] = (JsPath \ "userId").read[Long]
    val nameResult: JsResult[Long] = request.body.validate[Long](nameReads)
    nameResult match {
      case s: JsSuccess[Long] =>
        println("Gick bra! Värde: " + s.get)
        val id =  s.get
        val users = Users.getUserById(id)
        users.map(list =>
          list match {
            case v: List[User] if v.size > 0 =>
              val user = v.head
              println("Get User: before returning")
              println(user)
              println(user.name)
              println(user.password)
              Ok(Json.obj("status" -> "Success", "userId" -> s.get, "userName" -> user.name, "userPassword" -> user.password))
            case _ => {
              println("It Failed!!!")
              Ok(Json.obj("status" -> "Failure"))
            }
          })


      case e: JsError => {
        println("Gick skit")
        Future {Ok}
      }

      case _ => {
        println("FEEEL!")
        Future {Ok}
      }
    }
  }


/*  def getUser = Action.async(parse.json) { implicit request =>
    println("Get single user function")
    request.body.validate[(String, String)].map{
      case (name, password) =>
        val user = User(None, name, password)
        println(new Date().getTime() + ": " + user)
        val futureExist = Users.login(user)
        futureExist.map(v =>
          v match {
            case l: List[User] if l.size > 0 =>
              val user = l.head
              Ok(Json.obj("status" -> "Success", "userId" -> user.id, "userName" -> user.name, "userPassword" -> user.password))

            case _ => Ok(Json.obj("status" -> "Failure"))
          }
        )


    }.recoverTotal{ e =>
      Future{BadRequest}
    }
  }*/


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
