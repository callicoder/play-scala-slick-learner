package controllers

import play.api.mvc._
import play.api.libs.json.Json
import com.google.inject.{Inject, Singleton}
import controllers.responses._
import models.EventRepo
import models.Event
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

@Singleton
class EventController @Inject() (eventRepo: EventRepo) extends Controller {
	def findAll() = Action.async { request =>
		eventRepo.findAll().map { events =>
			Ok(Json.toJson(SuccessResponse(events)))			
		}
	}

	def findById(eventId: Long) = Action.async { request =>
		eventRepo.findById(eventId).map { event =>
			event.fold {
				NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No event found")))
			} { e =>
				Ok(Json.toJson(SuccessResponse(e)))
			}
		}
	}

	def create = Action.async(parse.json) { request =>

		val incomingEvent = request.body.validate[Event]
		incomingEvent.fold( error => {
			val errorMessage = s"Invalid JSON: ${error}"
			val response = ErrorResponse(ErrorResponse.INVALID_JSON, errorMessage)
			Future.successful(BadRequest(Json.toJson(response)))
		}, { event =>
			eventRepo.create(event).map { createdEvent =>
				Created(Json.toJson(SuccessResponse(createdEvent)))
		    }
		})
	}


}