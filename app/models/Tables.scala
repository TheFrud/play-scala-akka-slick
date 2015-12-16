package models

import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import slick.backend.DatabasePublisher
// import slick.driver.H2Driver.api._
import slick.driver.PostgresDriver.api._

/**
 * Created by Fredrik on 11-Oct-15.
 */
object Tables {

  case class User(id: Option[Long] = None, name: String, password: String)
  case class UserForm(name: String, password: String)
  case class UserIdForm(id: Long)
  case class SaveProfileForm(id: Long, oldName: String, name: String, password: String)
  case class UserRegistrationForm(name: String, password: String)

  class Users(tag: Tag) extends Table[User] (tag, "USERS") {
    def id = column[Long]("USER_ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("USER_ID_NAME")
    def password = column[String]("USER_ID_PASSWORD")

    // def * = (id.?, name, password)
    def * = (id.?, name, password) <> (User.tupled, User.unapply)
  }

  object Users {

    val db = Database.forConfig("postdb")
    def setupdb = {
      try{

        val setup = DBIO.seq(
          // Create the tables, including primary and foreign keys
          users.schema.create,

          users ++= Seq(
            User(None, "Frud", "pass1"),
            User(None, "Olle", "pass2"),
            User(None, "Patrik", "pass3")
          )

        )
        Await.result(db.run(setup), Duration.Inf)

      }finally println("DB Setup!")
    }

    def login(User: User) = {
      try{
        val users: Future[List[User]] = getAll
        val filteredusers: Future[List[User]] = users.map{
          case l: List[User] => l.filter(u => u.name == User.name && u.password == User.password)
        }
        filteredusers.map{
          case l: List[User] => l
        }


      } finally 1
    }

    def checkUserNameExistance(User: User) = {
      try{
        val users: Future[List[User]] = getAll
        val filteredusers: Future[List[User]] = users.map{
          case l: List[User] => l.filter(u => u.name == User.name && u.id != User.id)
        }
        filteredusers.map{
          case l: List[User] => l
        }


      } finally 1
    }

    def checkUserNameExistanceOnlyName(User: User) = {
      try{
        val users: Future[List[User]] = getAll
        val filteredusers: Future[List[User]] = users.map{
          case l: List[User] => l.filter(u => u.name == User.name)
        }
        filteredusers.map{
          case l: List[User] => l
        }


      } finally 1
    }

/*    def login(User: User) = {
      try{
        val users: Future[List[User]] = getAll
        val filteredusers: Future[List[User]] = users.map{
          case l: List[User] => l.filter(u => u.name == User.name && u.password == User.password)
        }
        filteredusers.map{
          case l: List[User] if l.size > 0 => true
          case _ => false
        }


      } finally 1
    }*/

    def create(User: User){
      try{
        val query = users ++= Seq(User)
        val future = db.run(query)
        future onSuccess {
          case something => println(something)
        }

        // future.foreach(r => println("Inserted rows: " + r))
      }finally 1

    }

    def getAll: Future[List[User]] = {
      try{
        val query = users.result
        val future = db.run(query)
        val list = future.map(f => f.toList)
        list

      }finally 1
    }

    def getAllNamed(name: String): Future[List[User]] = {
      try{
        val users: Future[List[User]] = getAll
        val filteredusers: Future[List[User]] = users.map{
          case l: List[User] => l.filter{_.name == name}
        }
        filteredusers.map{
          case l: List[User] => l
        }
      }finally 1
    }

    def getUserById(id: Long): Future[List[User]] = {
      try{
        val users: Future[List[User]] = getAll
        val filteredusers: Future[List[User]] = users.map{
          case l: List[User] => {
            println("Before filtering: " + l)
            l.filter{_.id == Some(id)}
          }
        }
        filteredusers.map{
          case l: List[User] => {
            println("Got user list from db")
            println("After filtering: " + l)
            l
          }
        }
      }finally 1
    }

    // Ska nog returnera boolean?
    def updateUser(id: Long, newName: String, newPassword: String) = {
      try{

        val q = users.filter(_.id === id)
          .map(p => (p.name,p.password))
          .update((newName,newPassword))

        val result = db.run(q)
        result.map(r => println(r))

/*        val q = for { user <- Tables.users if user.id === id } yield user
        val updateAction = q.update(User(Some(id),newName, newPassword))
        db.run(updateAction)*/

        /*
        val users = getUserById(id)
        users.map(list =>
          list match {
            case list: List => {
              println("List: " + list)
              val user = list.head
              println("Head: " + user)
              users.
            }
            case _ => println("Error: updateUser-method")
          }
          )
          */


      } finally 1
    }



  }

  val users = TableQuery[Users]


}
