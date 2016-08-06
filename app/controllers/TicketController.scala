package controllers

import com.google.inject.{Inject, Singleton}
import controllers.responses.{ErrorResponse, SuccessResponse}
import models.{TicketBlock, TicketBlockRepo}
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

@Singleton
class TicketController @Inject() (ticketBlockRepo: TicketBlockRepo) extends Controller {

	def findAll = Action.async { request =>
		ticketBlockRepo.findAll().map { ticketBlocks =>
			Ok(Json.toJson(SuccessResponse(ticketBlocks)))
		}
	}

	def findById(blockId: Long) = Action.async { request =>
		ticketBlockRepo.findById(blockId).map { ticketBlock =>
			ticketBlock.fold {
				NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No ticket block found")))
			} { tb =>
				Ok(Json.toJson(tb))
			}
		}
	}

	def create = Action.async(parse.json) { request =>
		val incomingTicketBlock = request.body.validate[TicketBlock]
		incomingTicketBlock.fold(error => {
			val errorMessage = s"Invalid JSON: ${error}"
			val response = ErrorResponse(ErrorResponse.INVALID_JSON, errorMessage)
			Future.successful(BadRequest(Json.toJson(response)))
		}, { ticketBlock =>
			ticketBlockRepo.create(ticketBlock).map { tb =>
				Created(Json.toJson(SuccessResponse(tb)))
			}
		})
	}
}