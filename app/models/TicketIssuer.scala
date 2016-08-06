package models

import akka.actor.Actor
import akka.actor.Status.{ Failure => ActorFailure }
import com.google.inject.Inject

/**
  * Created by rajeev on 6/8/16.
  */

case class InsufficientTicketsAvailable(ticketBlockID: Long,
                                         ticketsAvailable: Int) extends Throwable


class TicketIssuer @Inject() (ticketBlockRepo: TicketBlockRepo, orderRepo: OrderRepo) extends Actor {

  def placeOrder(order: Order) = {
    val origin = sender

    ticketBlockRepo.availability(order.ticketBlockId).map { availability =>
      if(availability >= order.ticketQuantity) {
        orderRepo.create(order).map { createdOrder =>
          origin ! createdOrder
        }
      } else {
        val failureResponse = InsufficientTicketsAvailable(
          order.ticketBlockId,
          availability)

        origin ! ActorFailure(failureResponse)
      }
    }
  }

  def receive = {
    case order: Order => placeOrder(order)
  }
}
