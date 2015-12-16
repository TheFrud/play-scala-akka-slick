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

  def registerUser = Action.async(parse.json) {implicit request =>
    println("Trying to register a user")

    implicit val userRegistration: Reads[UserRegistrationForm] = (
      (JsPath \ "name").read[String] and
        (JsPath \ "password").read[String]
      )(UserRegistrationForm.apply _)

    request.body.validate[UserRegistrationForm] match {
      case s: JsSuccess[UserRegistrationForm] => {
        val user = User(None, s.value.name, s.value.password)
        val futureExist = Users.checkUserNameExistanceOnlyName(user)
        futureExist.map(result =>
          result match{
            case l:List[User] if l.size > 0 =>
              // Användaren existerade
              Ok(Json.obj("status" -> "Failure", "msg" -> "Email already exists."))
            case _ =>
              // Användaren existerade inte
              Users.create(user)
              Ok(Json.obj("status" -> "Success"))

          }
        )

      }
      case e: JsError => {
        println(e)
        Future {Ok(Json.obj("status" -> "Failure"))}
      }
    }
  }

  // NYTT FÖRSÖK DÄR JAG ÄVEN TITTAR SÅ ATT ANVÄNDAREN EJ EXISTERAR REDAN
  def saveProfileSettings = Action.async(parse.json) {implicit request =>
    println("Save Profile Settings Method.")

    implicit val profileSettingsRead: Reads[SaveProfileForm] = (
      (JsPath \ "userId").read[Long] and
        (JsPath \ "oldUserName").read[String] and
        (JsPath \ "newUserName").read[String] and
        (JsPath \ "newUserPassword").read[String]
      )(SaveProfileForm.apply _)

    request.body.validate[SaveProfileForm] match {
      case s: JsSuccess[SaveProfileForm] => {

        val user = User(Some(s.value.id), s.value.name, s.value.password)
        val doesUserExist = Users.checkUserNameExistance(user)
        doesUserExist.map(list =>
          list match {
            case v: List[User] if v.size < 1 =>
              println("Validated: User don't exist. Updating DB...")
              Users.updateUser(s.value.id, s.value.name, s.value.password)
              Ok(Json.obj("status" -> "Success"))

            case _ => {
              println("NOT Validated: User already exists.")
              Ok
            }

      })
      }
      case e: JsError => {
        println(e)
        Future{Ok}
      }
    }

  }

/*
  def saveProfileSettings = Action.async(parse.json) {implicit request =>
    println("Save Profile Settings Method.")

    implicit val profileSettingsRead: Reads[SaveProfileForm] = (
      (JsPath \ "userId").read[Long] and
        (JsPath \ "oldUserName").read[String] and
        (JsPath \ "newUserName").read[String] and
        (JsPath \ "newUserPassword").read[String]
      )(SaveProfileForm.apply _)

    request.body.validate[SaveProfileForm] match {
      case s: JsSuccess[SaveProfileForm] => {

        val user = User(None, s.value.name, s.value.password)
        val doesUserExist = Users.checkUserNameExistance(user)
        doesUserExist.map(list =>
          list match {
            case v: List[User] if v.size < 1=>
              val user = v.head
              if(user.name.equals(s.value.oldName)){
                println("User doesn't exist. Updating...")
                Users.updateUser(s.value.id, s.value.name, s.value.password)
                Ok(Json.obj("status" -> "Success"))
                // Future{Ok(Json.obj("status" -> "Success"))}
              }
              Ok

            case _ => {
              println("User already exist. Shit.")
              println("Got info => Name = " + s.value.name + ", Password = " + s.value.password + ".")
              Ok
            }


          })
      }
      case e: JsError => {
        println(e)
        Future{Ok}
      }
    }

  }
  */


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
    futureUsers.map {users => Ok(Json.obj("status" -> "Success", "users" -> users))}
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

  def echoWs = WebSocket.acceptWithActor[String, JsValue]{
    request => out => Props(classOf[ChatSystemActor], out)
  }

  def androidWs = WebSocket.acceptWithActor[String, String]{
    request => out => Props(classOf[AndroidActor], out)
  }


}
