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

  case class Person(id: Option[Long] = None, name: String, age: Int)
  case class PersonForm(name: String, age: Int)

  class Persons(tag: Tag) extends Table[Person] (tag, "PERSONS") {
    def id = column[Long]("PERSON_ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("PERSON_NAME")
    def age = column[Int]("PERSON_AGE")

    // def * = (id.?, name, age)
    def * = (id.?, name, age) <> (Person.tupled, Person.unapply)
  }

  object Persons {

    val db = Database.forConfig("postdb")
    def setupdb = {
      try{

        val setup = DBIO.seq(
          // Create the tables, including primary and foreign keys
          persons.schema.create,

          persons ++= Seq(
            Person(None, "Frud", 25),
            Person(None, "Olle", 30),
            Person(None, "Patrik", 50)
          )

        )
        Await.result(db.run(setup), Duration.Inf)

      }finally println("DB Setup!")
    }

    def create(person: Person) = {
      try{
        val query = persons ++= Seq(person)
        val future = db.run(query)
        future.foreach(r => println("Inserted rows: " + r))
      }finally 1

    }

    def getAll: Future[List[Person]] = {
      try{
        val query = persons.result
        val future = db.run(query)
        val list = future.map(f => f.toList)
        list

      }finally 1
    }

    def getAllNamed(name: String): Future[List[Person]] = {
      try{
        val persons: Future[List[Person]] = getAll
        val filteredPersons: Future[List[Person]] = persons.map{
          case l: List[Person] => l.filter{_.name == name}
        }
        filteredPersons.map{
          case l: List[Person] => l
        }
      }finally 1
    }

  }

  val persons = TableQuery[Persons]


}
