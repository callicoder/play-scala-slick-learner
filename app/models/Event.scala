package models
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import slick.lifted.{Rep, Tag}
import slick.driver.JdbcProfile
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import com.google.inject.{Inject, Singleton}

import scala.concurrent.Future
import util.SlickMapping.jodaDateTimeMapping
import play.api.libs.concurrent.Execution.Implicits._

case class Event (
	id: Option[Long],
	name: String,
	start: DateTime,
	end: DateTime,
	address: String,
	city: String,
	state: String,
	country: String
)

object Event {
	implicit val format: Format[Event] = Json.format[Event]
}

trait EventEntity { self: HasDatabaseConfigProvider[JdbcProfile] =>
	import driver.api._

	class EventTable(tag: Tag) extends Table[Event](tag, "EVENTS") {
		val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
		val name = column[String]("NAME")
		val start = column[DateTime]("START")
		val end = column[DateTime]("END")
		val address = column[String]("ADDRESS")
		val city = column[String]("CITY")
		val state = column[String]("STATE")
		val country = column[String]("COUNTRY")

		def * = (id.?, name, start, end, address, city, state, country) <>
			((Event.apply _).tupled, Event.unapply)
	}
}

@Singleton()
class EventRepo @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)  extends EventEntity
	with HasDatabaseConfigProvider[JdbcProfile] {

	import driver.api._

	lazy protected val eventTableQuery = TableQuery[EventTable]

	lazy protected val eventTableQueryInc = eventTableQuery returning eventTableQuery.map(_.id)

	def findAll(): Future[Seq[Event]] = db.run {
		eventTableQuery.result
	}

	def findById(eventId: Long): Future[Option[Event]] = db.run {
		eventTableQuery.filter { e =>
			e.id === eventId
		}.result.headOption
	}

	def create(newEvent: Event): Future[Event] = {
		val insertion = eventTableQueryInc += newEvent
		val insertedIdFuture = db.run(insertion)
		val createdCopy: Future[Event] = insertedIdFuture.map { resultId =>
			newEvent.copy(id = Option(resultId))
		}
		createdCopy
	}

}
