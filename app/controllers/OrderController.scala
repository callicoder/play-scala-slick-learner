package controllers

import com.google.inject.{Inject, Singleton}
import controllers.responses.{ErrorResponse, SuccessResponse}
import models.{Order, OrderRepo, TicketBlockRepo}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Created by rajeev on 6/8/16.
  */

@Singleton
class OrderController @Inject() (orderRepo: OrderRepo, ticketBlockRepo: TicketBlockRepo) extends Controller {
  def findAll = Action.async { request =>
    orderRepo.findAll().map(orders =>
      Ok(Json.toJson(SuccessResponse(orders)))
    )
  }

  def findById(orderId: Long) = Action.async { request =>
    orderRepo.findById(orderId).map { order =>
      order.fold {
        NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No order found")))
      } { o =>
        Ok(Json.toJson(SuccessResponse(o)))
      }
    }
  }

  def create = Action.async(parse.json) { request =>
    val incomingOrder = request.body.validate[Order]

    incomingOrder.fold ( error => {
      val errorMessage = s"Invalid JSON: ${error}"
      val response = ErrorResponse(ErrorResponse.INVALID_JSON, errorMessage)
      Future.successful(BadRequest(Json.toJson(response)))
    }, { order =>
      ticketBlockRepo.availability(order.ticketBlockId).flatMap { availability =>
        if(availability >= order.ticketQuantity) {
          orderRepo.create(order).map { co =>
            Created(Json.toJson(SuccessResponse(co)))
          }
        } else {
          val responseMessage = "There are not enough tickets remaining to complete this order." +
            s" Quantity Remaining: ${availability}"
          val response = ErrorResponse(
            ErrorResponse.NOT_ENOUGH_TICKETS,
            responseMessage)
          Future.successful(BadRequest(Json.toJson(response)))
        }
      }
    })
  }
}
