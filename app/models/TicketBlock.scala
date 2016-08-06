package models

import com.google.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
import play.api.libs.json.Format
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits._
import util.SlickMapping.jodaDateTimeMapping

import scala.concurrent.Future

case class TicketBlock (
	id: Option[Long],
	eventId: Long,
	name: String,
	productCode: String,
	price: BigDecimal,
	initialSize: Int,
	saleStart: DateTime,
	saleEnd: DateTime
)

object TicketBlock {
	implicit val format: Format[TicketBlock] = Json.format[TicketBlock]
}

trait TicketBlockEntity extends EventEntity { self: HasDatabaseConfigProvider[JdbcProfile] =>
	import driver.api._

	class TicketBlockTable(tag: Tag) extends Table[TicketBlock](tag, "TICKET_BLOCKS") {
		val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
		val eventId = column[Long]("EVENT_ID")
		val name = column[String]("NAME")
		val productCode = column[String]("PRODUCT_CODE")
		val price = column[BigDecimal]("PRICE")
		val initialSize = column[Int]("INITIAL_SIZE")
		val saleStart = column[DateTime]("SALE_START")
		val saleEnd = column[DateTime]("SALE_END")

		val event = foreignKey("TB_EVENT", eventId, TableQuery[EventTable])(_.id)

		def * = (id.?, eventId, name, productCode, price, initialSize,
			saleStart, saleEnd) <>
			((TicketBlock.apply _).tupled, TicketBlock.unapply)
	}

}

@Singleton()
class TicketBlockRepo @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends TicketBlockEntity
	with HasDatabaseConfigProvider[JdbcProfile] {

	import driver.api._

	lazy protected val ticketBlockTableQuery = TableQuery[TicketBlockTable]

	lazy protected val ticketBlockTableQueryInc = ticketBlockTableQuery returning ticketBlockTableQuery.map(_.id)

	def findAll(): Future[Seq[TicketBlock]] = db.run {
		ticketBlockTableQuery.result
	}

	def findById(blockId: Long): Future[Option[TicketBlock]] = {
		val ticketBlockById = ticketBlockTableQuery.filter(_.id === blockId).result.headOption
		db.run(ticketBlockById)
	}

	def create(newTicketBlock: TicketBlock): Future[TicketBlock] = {
		val insertion = ticketBlockTableQueryInc += newTicketBlock
		db.run(insertion).map { resultId =>
			newTicketBlock.copy(id = Option(resultId))
		}
	}

	def availability(ticketBlockId: Long): Future[Int] = {
		db.run {
			val query = sql"""
				SELECT INITIAL_SIZE - COALESCE(SUM(TICKET_QUANTITY), 0)
    		FROM TICKET_BLOCKS tb
      	LEFT JOIN ORDERS o ON o.TICKET_BLOCK_ID=tb.ID
        WHERE tb.ID=${ticketBlockId}
        GROUP BY INITIAL_SIZE;
				""".as[Int]

			query.headOption
		}.map {_.getOrElse(0)}
	}
}