package models

import com.google.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Format, Json}
import slick.driver.JdbcProfile
import util.SlickMapping.jodaDateTimeMapping
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Created by rajeev on 5/8/16.
  */
case class Order (id: Option[Long],
                  ticketBlockId: Long,
                  customerName: String,
                  customerEmail: String,
                  ticketQuantity: Int,
                  timestamp: Option[DateTime]
                 )


object Order {
  implicit val format: Format[Order] = Json.format[Order]
}

trait OrderEntity extends TicketBlockEntity { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import driver.api._

  class OrderTable(tag: Tag) extends Table[Order](tag, "ORDERS") {
    val id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    val ticketBlockId = column[Long]("TICKET_BLOCK_ID")
    val customerName = column[String]("CUSTOMER_NAME")
    val customerEmail = column[String]("CUSTOMER_EMAIL")
    val ticketQuantity = column[Int]("TICKET_QUANTITY")
    val timestamp = column[DateTime]("TIMESTAMP")

    val ticketBlock = foreignKey("O_TICKETBLOCK", ticketBlockId, TableQuery[TicketBlockTable])(_.id)

    def * = (id.?, ticketBlockId, customerName, customerEmail, ticketQuantity, timestamp.?) <>
      ((Order.apply _).tupled, Order.unapply)
  }
}


@Singleton
class OrderRepo @Inject() (protected  val dbConfigProvider: DatabaseConfigProvider) extends OrderEntity
with HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  val orderTableQuery = TableQuery[OrderTable]

  def findAll(): Future[Seq[Order]] = db.run {
    orderTableQuery.result
  }

  def findById(orderId: Long): Future[Option[Order]] = db.run {
    orderTableQuery.filter(_.id === orderId).result.headOption
  }

  def create(newOrder: Order): Future[Order] = {
    val nowStamp = new DateTime()
    val newOrderWithTimestamp = newOrder.copy(timestamp = Option(nowStamp))
    val insertion = (orderTableQuery returning orderTableQuery.map(_.id)) += newOrderWithTimestamp
    db.run(insertion).map { resultId =>
      newOrderWithTimestamp.copy(id = Option(resultId))
    }
  }


}
